package cn.irina.thepitaddon.events

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.runnable.Maintenance.Companion.isInMaintenanceMode
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.random.RandomUtil.random
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class MaintenanceListener : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        if (player.hasPermission("pit.admin")) return
        if (!profile.isInArena || "afk" == player.world.name) return
        if (!isInMaintenanceMode()) return
        resetPlayerCombatTime(player)
        doRespawn(player)
        player.playSound(player.location, Sound.VILLAGER_NO, 1f, 1f)
        player.sendMessage(CC.translate("&c服务器维护中, 请稍后再试!"))
    }

    private fun resetPlayerCombatTime(player: Player) {
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        profile.combatTimer = Cooldown(0)
    }

    private fun doRespawn(player: Player) {
        val location = ThePit.getInstance().pitConfig
            .spawnLocations[random.nextInt(ThePit.getInstance().pitConfig.spawnLocations.size)]
        player.teleport(location)
    }
}