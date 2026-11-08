package cn.irina.thepitaddon.command.player

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player

/**
 * 挂机池传送指令。
 * /afk      进入挂机池（自动创建 afk 世界）
 * /afk leave 离开挂机池，返回主世界出生点
 */
@Command(name = "afk")
class AfkCommand {

    @Execute
    fun onDefault(@Context player: Player) {
        if (player.world.name.equals("afk", ignoreCase = true)) {
            leaveAfk(player)
            return
        }
        enterAfk(player)
    }

    @Execute(name = "leave")
    fun onLeave(@Context player: Player) {
        leaveAfk(player)
    }

    companion object {
        @JvmStatic
        fun enterAfk(player: Player) {
        val afkWorld: World? = Bukkit.getWorld("afk") ?: WorldCreator("afk")
            .type(WorldType.FLAT)
            .generateStructures(false)
            .createWorld()
        if (afkWorld == null) {
            player.sendMessage(CC.translate("&c挂机池世界创建失败, 请联系管理员!"))
            return
        }
        afkWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        afkWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        afkWorld.setGameRule(GameRule.DO_FIRE_TICK, false)
            afkWorld.difficulty = Difficulty.PEACEFUL
            player.teleport(afkWorld.spawnLocation)
            player.sendMessage(CC.translate("&a你已进入挂机池, 每5分钟获得挂机奖励!"))
        }

        @JvmStatic
        fun leaveAfk(player: Player) {
            val spawn = ThePit.getInstance().pitConfig.spawnLocations.firstOrNull()
            val target = spawn ?: Bukkit.getWorld("world")?.spawnLocation ?: player.location
            player.teleport(target)
            player.sendMessage(CC.translate("&a你已离开挂机池!"))
        }
    }
}
