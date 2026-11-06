package net.mizukilab.pit.util

import org.bukkit.Bukkit

class BoomFirework(world: World) :
    EntityFireworks(world) {

    val players = Bukkit.getOnlinePlayers()
    var gone = false
    override fun t_() {
        if (gone) {
            die()
            return
        }
        if (!world.isClientSide) {
            gone = true
            if (players.isNotEmpty()) {
                for (player in players) {
                    (player as CraftPlayer).handle.playerConnection.sendPacket(
                        PacketPlayOutEntityStatus(this, 17.toByte())
                    )
                }
            } else {
                world.broadcastEntityEffect(this, 17.toByte())
                die()
            }
        }
    }
}