package net.mizukilab.pit.command

import cn.charlotte.pit.ThePit
import cn.mythicland.npclib.bot.FighterRobot
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.RootCommand
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.optional.OptionalArg
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * 机器人管理命令。
 *
 * <p>机器人由 npclib 的 BotManager 创建（真实登录的假玩家），在游戏里表现为可被攻击/击杀的玩家。
 * 优先使用 {@code plugins/ThePitUltimate/bots.yml} 里配置的出生点；没有配置时在命令执行者脚下生成。</p>
 */
@RootCommand
class BotCommands {

    companion object {
        private val bots = CopyOnWriteArrayList<FighterRobot>()
        private val counter = AtomicInteger()

        @JvmStatic
        fun clearAll(): Int {
            val size = bots.size
            bots.forEach {
                runCatching { it.close() }
            }
            bots.clear()
            return size
        }

        @JvmStatic
        fun count(): Int = bots.size
    }

    @Execute(name = "botspawn")
    @Permission("pit.admin")
    fun spawn(@Context player: Player, @OptionalArg("count") count: Int?) {
        val amount = (count ?: 1).coerceIn(1, 10)
        val locations = readSpawnPoints()
        val lib = ThePit.getInstance().npcFactory.npcLib
        var spawned = 0
        repeat(amount) { index ->
            val base = locations.getOrNull(index) ?: player.location.clone()
            val location = safeLocation(base, player)
            val name = "Bot-" + counter.incrementAndGet()
            try {
                bots.add(lib.createFighterRobot(location) { options ->
                    options.name(name)
                    // 死亡后 10 秒重生；-1 表示不重生
                    options.respawnDelay(200)
                    options.radius(8.0)
                    options.movementSpeed(1.0)
                })
                spawned++
            } catch (throwable: Throwable) {
                player.sendMessage(CC.translate("&c生成机器人失败: &7" + throwable.message))
            }
        }
        player.sendMessage(CC.translate(
            "&a已生成 &e$spawned &a个机器人（当前共 &e${bots.size}&a 个）。" +
                "&7提示：机器人只会在附近 32 格内有真人玩家时移动/战斗。"
        ))
    }

    @Execute(name = "botclear")
    @Permission("pit.admin")
    fun clear(@Context player: Player) {
        val removed = clearAll()
        player.sendMessage(CC.translate("&a已移除 &e$removed &a个机器人。"))
    }

    @Execute(name = "botlist")
    @Permission("pit.admin")
    fun list(@Context player: Player) {
        val alive = bots.count { robot -> runCatching { robot.entity().bukkitEntity.isValid }.getOrDefault(false) }
        player.sendMessage(CC.translate("&7机器人: &e${bots.size} &7个（存活 &e$alive&7）"))
        bots.take(10).forEach { robot ->
            val entity = robot.entity().bukkitEntity
            val world = entity.world?.name ?: "?"
            player.sendMessage(CC.translate(
                "&8- &f${entity.name} &7@ $world " +
                    String.format("%.1f, %.1f, %.1f", entity.location.x, entity.location.y, entity.location.z)
            ))
        }
    }

    private fun readSpawnPoints(): List<Location> {
        val file = File(ThePit.getInstance().dataFolder, "bots.yml")
        if (!file.isFile) return emptyList()
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("bots") ?: return emptyList()
        return section.getKeys(false).mapNotNull { key ->
            val world = section.getString("$key.world") ?: return@mapNotNull null
            val bukkitWorld = Bukkit.getWorld(world) ?: return@mapNotNull null
            Location(
                bukkitWorld,
                section.getDouble("$key.x"),
                section.getDouble("$key.y"),
                section.getDouble("$key.z"),
                section.getDouble("$key.yaw").toFloat(),
                section.getDouble("$key.pitch").toFloat()
            )
        }
    }

    private fun safeLocation(base: Location, fallback: Player): Location {
        val world = base.world ?: return fallback.location.clone()
        val location = Location(world, base.x, base.y, base.z, base.yaw, base.pitch)
        // 出生点方块被占时抬高 1 格，避免机器人卡在方块里
        if (!world.getBlockAt(location).isPassable) {
            location.add(0.0, 1.0, 0.0)
        }
        return location
    }
}
