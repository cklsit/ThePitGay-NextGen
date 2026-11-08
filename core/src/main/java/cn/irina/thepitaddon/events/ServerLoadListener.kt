package cn.irina.thepitaddon.events

import cn.irina.thepitaddon.Main
import net.mizukilab.pit.util.chat.CC
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent

class ServerLoadListener : Listener {
    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        if (Main.instance.isServerLoaded()) return
        event.disallow(
            PlayerLoginEvent.Result.KICK_OTHER, CC.translate("&c正在加载ThePitAddon...")
        )
    }
}