package net.mizukilab.pit.command

import cn.charlotte.pit.ThePit
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.RootCommand
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.optional.OptionalArg
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.npc.PitBotConfiguration
import net.mizukilab.pit.npc.PitBotRoster
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.Locale

@RootCommand
@Permission("pit.admin")
class BotCommands {
    @Execute(name = "bot")
    fun help(@Context sender: CommandSender) {
        sender.reply("&e/bot list [页码] &7· &e/bot info <ID> &7· &e/bot reload")
        sender.reply("&e/bot spawn|despawn|enable|disable <ID> &7· &e/bot start|stop")
        sender.reply("&e/bot add|move <ID> &7· &e/bot mode <ID> <patrol|fight-back|aggressive>")
        sender.reply("&e/botspawn [数量] &7· &e/botlist [页码] &7· &e/botclear")
    }

    @Execute(name = "bot list")
    fun list(@Context sender: CommandSender, @OptionalArg("page") page: Int?) = run(sender) {
        val definitions = definitions()
        val ids = definitions.map { it.id } + temporaryIds()
        val alive = ids.count { status(it) == "存活" }
        val pages = ((ids.size + 14) / 15).coerceAtLeast(1)
        val current = (page ?: 1).coerceIn(1, pages)
        sender.reply("&7Bot 配置 &e${definitions.size} &7个，临时 &e${temporaryIds().size} &7个，存活 &e$alive &7个（$current/$pages）")
        for (id in ids.drop((current - 1) * 15).take(15)) sender.reply("&8- &f$id &7${status(id)}")
    }

    @Execute(name = "botlist")
    fun legacyList(@Context sender: CommandSender, @OptionalArg("page") page: Int?) = list(sender, page)

    @Execute(name = "bot info")
    fun info(@Context sender: CommandSender, @Arg("id") id: String) = run(sender) {
        val bot = definition(id)
        val position = bot.position
        sender.reply("&e$id &7${status(id)} &8· &f${bot.name} &8· &7${bot.mode.configName()}")
        sender.reply("&7出生点 &f${position.world} ${position.x} ${position.y} ${position.z} &8· " +
            "&7路线点 &f${bot.waypoints.size} &8· &7重生 &f${bot.respawnTicks} tick")
    }

    @Execute(name = "bot reload")
    fun reload(@Context sender: CommandSender) = run(sender) {
        sender.reply("&a已重载 bots.yml，共 ${reload()} 个 Bot 配置。")
    }

    @Execute(name = "bot spawn")
    fun spawn(@Context sender: CommandSender, @Arg("id") id: String) = run(sender) {
        spawn(id)
        sender.reply("&aBot $id &7${status(id)}")
    }

    @Execute(name = "bot despawn")
    fun despawn(@Context sender: CommandSender, @Arg("id") id: String) = run(sender) {
        despawn(id)
        sender.reply("&a已收回 Bot $id。")
    }

    @Execute(name = "bot start")
    fun start(@Context sender: CommandSender) = run(sender) {
        resume()
        sender.reply("&aBot 已恢复按 bots.yml 自动生成。")
    }

    @Execute(name = "bot stop")
    fun stop(@Context sender: CommandSender) = run(sender) {
        sender.reply("&a已收回 ${clear()} 个 Bot 并暂停自动生成。用 /bot start 恢复。")
    }

    @Execute(name = "botclear")
    fun legacyClear(@Context sender: CommandSender) = stop(sender)

    @Execute(name = "bot enable")
    fun enable(@Context sender: CommandSender, @Arg("id") id: String) = setEnabled(sender, id, true)

    @Execute(name = "bot disable")
    fun disable(@Context sender: CommandSender, @Arg("id") id: String) = setEnabled(sender, id, false)

    private fun setEnabled(sender: CommandSender, id: String, enabled: Boolean) = run(sender) {
        definition(id)
        update(id) { yaml, path -> yaml.set("$path.enabled", enabled) }
        sender.reply("&a已${if (enabled) "启用" else "禁用"} Bot $id。")
    }

    @Execute(name = "bot mode")
    fun mode(@Context sender: CommandSender, @Arg("id") id: String, @Arg("mode") mode: String) = run(sender) {
        definition(id)
        val selected = PitBotConfiguration.mode(mode).configName()
        update(id) { yaml, path -> yaml.set("$path.mode", selected) }
        sender.reply("&aBot $id 的模式已设为 $selected。")
    }

    @Execute(name = "bot add")
    fun add(@Context player: Player, @Arg("id") id: String) = run(player) {
        update(id) { yaml, path ->
            require(!yaml.contains(path)) { "Bot $id 已存在" }
            writePosition(yaml, path, player.location)
        }
        player.reply("&a已在当前位置添加 Bot $id。")
    }

    @Execute(name = "bot move")
    fun move(@Context player: Player, @Arg("id") id: String) = run(player) {
        definition(id)
        update(id) { yaml, path -> writePosition(yaml, path, player.location) }
        player.reply("&a已更新 Bot $id 的出生点。")
    }

    @Execute(name = "botspawn")
    fun spawnTemporary(@Context sender: CommandSender, @OptionalArg("count") count: Int?) = run(sender) {
        val amount = temporarySpawn((count ?: 1).coerceIn(1, 10), (sender as? Player)?.location)
        sender.reply("&a已生成 $amount 个临时对战 Bot。")
    }

    private fun writePosition(yaml: YamlConfiguration, path: String, position: Location) {
        yaml.set("$path.world", position.world.name)
        yaml.set("$path.x", position.x)
        yaml.set("$path.y", position.y)
        yaml.set("$path.z", position.z)
        yaml.set("$path.yaw", position.yaw.toDouble())
        yaml.set("$path.pitch", position.pitch.toDouble())
    }

    private fun run(sender: CommandSender, action: PitBotRoster.() -> Unit) {
        try {
            checkNotNull(ThePit.getInstance().botRoster) { "Bot 模块正在加载" }.action()
        } catch (failure: Exception) {
            sender.reply("&cBot 操作失败：${failure.message}")
        }
    }

    private fun Enum<*>.configName(): String = name.lowercase(Locale.ROOT).replace('_', '-')
    private fun CommandSender.reply(text: String) = sendMessage(CC.translate(text))
}
