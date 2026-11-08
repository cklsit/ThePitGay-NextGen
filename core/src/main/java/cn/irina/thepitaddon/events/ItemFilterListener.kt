package cn.irina.thepitaddon.events

import cn.charlotte.pit.event.PitKillEvent
import cn.irina.thepitaddon.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

class ItemFilterListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Main.instance.server.scheduler.runTaskLater(Main.instance, {
            Main.instance.getFilterManager().applyFilters(event.player)
        }, 20L)
    }
    
    @EventHandler
    fun onPlayerSpawn(event: PlayerTeleportEvent) {
        Main.instance.server.scheduler.runTaskLater(Main.instance, {
            Main.instance.getFilterManager().applyFilters(event.player)
        }, 20L)
    }
}