package cn.irina.thepitaddon.command.admin

import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.runnable.Maintenance
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@Command(name = "lock")
@Permission("pit.admin")
class LockCommand {
    @Execute
    fun handleLockCommand(@Context commandSender: Player) {
        val prefix = Main.instance.PREFIX
        val newMode = !Maintenance.isInMaintenanceMode()
        for (player in Bukkit.getOnlinePlayers()) {
            resetPlayerCombatTime(player)
        }
        Maintenance.setMaintenanceMode(newMode)
        if (newMode) {
            commandSender.sendMessage(CC.translate("$prefix&a已启动维护模式."))
        } else {
            commandSender.sendMessage(CC.translate("$prefix&c已关闭维护模式."))
        }
    }

    private fun resetPlayerCombatTime(player: Player) {
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        profile.combatTimer = Cooldown(0)
    }
}