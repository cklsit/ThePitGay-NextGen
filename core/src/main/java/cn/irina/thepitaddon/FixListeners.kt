package cn.irina.thepitaddon

import cn.charlotte.pit.data.PlayerProfile
import cn.charlotte.pit.event.PitStreakKillChangeEvent
import cn.irina.thepitaddon.manager.PitManager
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable


class FixListeners : Listener {
    private val blockTypes: MutableList<Material> = ArrayList()

    init {
        object : BukkitRunnable() {
            var playerMoveEvent: PlayerMoveEvent? = null
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    val loc = player.location
                    playerMoveEvent = PlayerMoveEvent(player, loc, loc)
                    Bukkit.getPluginManager().callEvent(playerMoveEvent)
                }
            }
        }.runTaskTimer(Main.instance, 0L, 2 * 60 * 20L)

        blockTypes.add(Material.HOPPER)
        blockTypes.add(Material.DISPENSER)
        blockTypes.add(Material.DROPPER)
        blockTypes.add(Material.ACTIVATOR_RAIL)
        blockTypes.add(Material.DETECTOR_RAIL)
        blockTypes.add(Material.POWERED_RAIL)
        blockTypes.add(Material.RAILS)
        blockTypes.add(Material.BREWING_STAND)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.hasPermission("pit.admin")) return

        val clickedBlock = event.clickedBlock ?: return

        if (!blockTypes.contains(clickedBlock.type)) return
        player.sendMessage(CC.translate("&c此方块不被允许使用!"))
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun launchArrowEvent(event: ProjectileLaunchEvent) {
        if (event.entity !is Arrow) return
        val arrow = event.entity as Arrow

        if (arrow.shooter !is Player) return
        val player = arrow.shooter as Player
        val profile = PlayerProfile.getRawCache(player.uniqueId)

        if (profile.isInArena) return
        event.isCancelled = true
        player.sendMessage(CC.translate("&c你不能在这里射箭!"))
        player.playSound(player.location, Sound.VILLAGER_NO, 1.0f, 1.0f)
    }

    @EventHandler
    fun onPitKill(event: PitStreakKillChangeEvent) {
        val myself = (Bukkit.getPlayer(event.playerProfile.playerUuid) ?: return) as CraftPlayer
        val myselfHearts = myself.handle.absorptionHearts
        if (myselfHearts >= LimitAbsorptionHearts) {
            myself.handle.absorptionHearts = LimitAbsorptionHearts
        }
    }


    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        if (!profile.isInArena) return
        if (!player.hasPermission("pit.streak")) return
        if (profile.streakKills >= 50.0 && profile.bounty > 0) return

        profile.streakKills = 50.0
        profile.bounty = 100
        player.sendMessage(CC.translate("&a你离开了保护区, 已自动为你开启超级连杀!"))

    }

    @EventHandler
    fun onPlayerWalkInAfkWorld(event: PlayerMoveEvent) {
        val player = event.player
        if (!player.world.name.equals("afk")) return
        if (!hasAmuletInInventory(player, "amulet_shadow")) return
        val location = player.location.add(0.0, 0.5, 0.0)
        player.world.players.forEach { targetPlayer ->
            sendRedstoneParticle(targetPlayer, location, 128f, 0f, 128f)
        }
    }

    private fun hasAmuletInInventory(player: Player, internalName: String): Boolean {
        return PitManager.hasInternalItem(player, internalName)
    }

    private fun sendRedstoneParticle(
        sender: Player,
        location: org.bukkit.Location,
        r: Float,
        g: Float,
        b: Float
    ) {
        val packet = net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles(
            net.minecraft.server.v1_8_R3.EnumParticle.REDSTONE,
            true,
            location.x.toFloat(),
            location.y.toFloat(),
            location.z.toFloat(),
            r / 255,
            g / 255,
            b / 255,
            1.0f,
            0
        )
        (sender as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    @EventHandler
    fun onArrowHit(event: ProjectileHitEvent) {
        val arrow = event.entity
        if (arrow is Arrow && arrow.shooter is Player) {
            arrow.remove()
        }
    }

    @EventHandler
    fun onPlayerUseRodInAfkWorld(event: PlayerInteractEvent) {
        val player = event.player
        if ("afk" != player.world.name) return
        if (player.inventory.itemInHand.type != Material.FISHING_ROD) return
        event.isCancelled = true
    }

    companion object {
        @JvmField
        val LimitAbsorptionHearts: Float = Main.instance.config?.getInt("LimitAbsorptionHearts")?.toFloat() ?: 120F
    }
}
