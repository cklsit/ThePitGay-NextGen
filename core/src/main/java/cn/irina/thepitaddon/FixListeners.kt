package cn.irina.thepitaddon

import cn.charlotte.pit.data.PlayerProfile
import cn.charlotte.pit.event.PitStreakKillChangeEvent
import cn.irina.thepitaddon.manager.PitManager
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.entity.CreatureSpawnEvent
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
                    Bukkit.getPluginManager().callEvent(playerMoveEvent!!)
                }
            }
        }.runTaskTimer(Main.instance.plugin, 0L, 2 * 60 * 20L)

        blockTypes.add(Material.HOPPER)
        blockTypes.add(Material.DISPENSER)
        blockTypes.add(Material.DROPPER)
        blockTypes.add(Material.ACTIVATOR_RAIL)
        blockTypes.add(Material.DETECTOR_RAIL)
        blockTypes.add(Material.POWERED_RAIL)
        blockTypes.add(Material.RAIL)
        blockTypes.add(Material.BREWING_STAND)
    }

    @EventHandler
    fun onMobSpawnInAfk(event: CreatureSpawnEvent) {
        if (event.location.world?.name?.equals("afk", ignoreCase = true) == true) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onCoinRedeem(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return
        val internal = ItemUtil.getInternalName(item) ?: return
        if (!internal.startsWith("coin_")) return
        val idx = internal.substringAfter("coin_").toIntOrNull() ?: return
        val value = when (idx) {
            1 -> 1000.0
            2 -> 10000.0
            3 -> 100000.0
            4 -> 1000000.0
            5 -> 10000000.0
            else -> return
        }
        event.isCancelled = true
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        profile.coins = profile.coins + value
        if (item.amount > 1) item.amount -= 1 else player.inventory.setItemInMainHand(null)
        player.sendMessage(CC.translate("&6你兑换了 &e" + value.toInt() + " &6硬币!"))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
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
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
    }

    @EventHandler
    fun onPitKill(event: PitStreakKillChangeEvent) {
        val myself = Bukkit.getPlayer(event.playerProfile.playerUuid) ?: return
        val myselfHearts = myself.absorptionAmount
        if (myselfHearts >= LimitAbsorptionHearts) {
            myself.absorptionAmount = LimitAbsorptionHearts.toDouble()
        }
    }


    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        if (!profile.isInArena) return
        if (!player.hasPermission("pit.streak")) return
        if (profile.streakKills >= 50.0 && profile.bounty > 0) return

        // 按所选超级连杀天赋设定连杀数：登峰造极需 400、月球之旅需 100，否则默认 50
        val targetStreak = when (profile.chosePerk[5]?.perkInternalName) {
            "uber_streak" -> 400.0
            "to_the_moon" -> 100.0
            else -> 50.0
        }
        profile.streakKills = targetStreak
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
        sender.spawnParticle(
            org.bukkit.Particle.DUST,
            location,
            1,
            0.0,
            0.0,
            0.0,
            0.0,
            org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(r.toInt(), g.toInt(), b.toInt()), 1.0f)
        )
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
        val LimitAbsorptionHearts: Float = Main.instance.cfg?.getInt("LimitAbsorptionHearts")?.toFloat() ?: 120F
    }
}