package cn.irina.thepitaddon.command.player

import net.mizukilab.pit.util.chat.CC
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

@Command(name = "suicide")
class PlayerSuicide {

    @Execute
    fun killMySelf(@Context player: Player) {
        if (checkSuicideNumber.getOrDefault(player.uniqueId, 0) != 1) {
            checkSuicideNumber[player.uniqueId] = 1
            player.sendMessage(CC.translate("&c注意! 你当前正在使用 &e/suicide &c指令自杀!"))
            player.sendMessage(CC.translate("&c使用此指令造成的一切后果本服管理组将不会承担任何责任!"))
            player.sendMessage(CC.translate("&c若你已了解清楚后果, 请在 &e8s &c内再次输入 &e/suicide &c以确认!"))
            if (task == null) {
                task = object : BukkitRunnable() {
                    override fun run() {
                        checkSuicideNumber.replace(player.uniqueId, 0)
                    }
                }
                (task as BukkitRunnable).runTaskLater(Main.instance, 8 * 20L)
            }
        } else {
            if (task != null) {
                task!!.cancel()
            }
            checkSuicideNumber.replace(player.uniqueId, 0)
            player.damage(player.maxHealth * 100)
        }
    }

    companion object {
        private val checkSuicideNumber = HashMap<UUID, Int>()
        private var task: BukkitRunnable? = null
    }
}
