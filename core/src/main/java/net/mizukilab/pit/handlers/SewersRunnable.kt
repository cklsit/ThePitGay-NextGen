package net.mizukilab.pit.handlers

import cn.charlotte.pit.ThePit
import net.mizukilab.pit.config.NewConfiguration
import net.mizukilab.pit.getPitProfile
import net.mizukilab.pit.item.MythicColor
import net.mizukilab.pit.item.type.mythic.MythicLeggingsItem
import net.mizukilab.pit.sendMultiMessage
import net.mizukilab.pit.util.RandomList
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random
object SewersRunnable : BukkitRunnable(), Listener {

    private var existSewersChest: List<Location>? = null
    private var lastClaimed = -1L

    var isParticle = false
    private var particleTask: BukkitRunnable? = null

    private val randomList = RandomList(
        "xp" to 90,
        "gold" to 90,
        "diamond_chestplate" to 50,
        "diamond_leggings" to 50,
        "diamond_boots" to 50,
        "rubbish" to 30,
        "mythic_leggings" to 3,
        "milk_buckets" to 10,
        "renown" to 3,
        "speed_eggs" to 5,
    )

    override fun run() {
        val locs = resolveChestLocations()
        if (locs.isEmpty()) return

        if (!isParticle || particleTask == null ) {
            isParticle = true
            particleTask = object : BukkitRunnable() {
                override fun run() {
                    if (!isParticle) {
                        cancel()
                        return
                    }
                    locs.forEach { loc ->
                        generateRandomParticlesAroundChest(loc, org.bukkit.Particle.HAPPY_VILLAGER)
                    }
                }
            }
            particleTask!!.runTaskTimer(ThePit.getInstance(), 0L, 10L)
        }

        if (System.currentTimeMillis() - lastClaimed < 1000 * NewConfiguration.sewersSpawn) return

        var count = 0
        locs.filter { it.block.type == Material.AIR }
            .forEach { loc ->
                loc.block.apply {
                    type = Material.CHEST
                    setMetadata("Sewers_Chest", FixedMetadataValue(ThePit.getInstance(), true))
                    count++
                }
                CC.boardCast("&9§l下水道! &7箱子点位${count}已刷新: x: ${loc.block.location.x}, y: ${loc.block.location.y}, z: ${loc.block.location.z}")
            }

        existSewersChest = locs
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock?.type == Material.CHEST) {
            if ((event.clickedBlock?.hasMetadata("Sewers_Chest") == true)) {
                event.clickedBlock?.let { claim(event.player, it.location) }
            }
        }
    }

    private var autoChestLocations: List<Location>? = null
    private var autoChestCenter: Location? = null

    private fun resolveChestLocations(): List<Location> {
        val configured = ThePit.getInstance().pitConfig.sewersChestsLocations
            .filter { it != null && it.world != null && it.y > 0 }
        if (configured.isNotEmpty()) return configured
        val center = ThePit.getInstance().pitConfig.sewersLocation ?: return emptyList()
        if (center.world == null || center.y <= 0) return emptyList()
        val cachedCenter = autoChestCenter
        val cached = autoChestLocations
        if (cached != null && cachedCenter != null && cachedCenter.world == center.world && cachedCenter.distanceSquared(center) < 4.0) {
            return cached
        }
        val generated = generateAutoChestLocations(center)
        ThePit.getInstance().logger.info("[Sewers] auto chest points=" + generated.size + " center=" + center);
        autoChestLocations = generated
        autoChestCenter = center.clone()
        return generated
    }

    private fun generateAutoChestLocations(center: Location): List<Location> {
        val world = center.world ?: return emptyList()
        val random = java.util.Random(center.blockX.toLong() * 31 + center.blockZ.toLong())
        val points = mutableListOf<Location>()
        var attempts = 0
        while (points.size < 8 && attempts < 200) {
            attempts++
            val dx = random.nextInt(-14, 15)
            val dz = random.nextInt(-14, 15)
            if (dx * dx + dz * dz < 16) continue
            val x = center.blockX + dx
            val z = center.blockZ + dz
            for (dy in -10..10) {
                val y = center.blockY + dy
                val ground = world.getBlockAt(x, y, z)
                val above = world.getBlockAt(x, y + 1, z)
                val above2 = world.getBlockAt(x, y + 2, z)
                if (!ground.type.isAir && above.type.isAir && above2.type.isAir) {
                    points.add(Location(world, x + 0.5, y + 1.0, z + 0.5))
                    break
                }
            }
        }
        if (points.isEmpty()) {
            for (dx in -3..3) {
                for (dz in -3..3) {
                    val x = center.blockX + dx
                    val z = center.blockZ + dz
                    for (dy in -10..10) {
                        val y = center.blockY + dy
                        val ground = world.getBlockAt(x, y, z)
                        val above = world.getBlockAt(x, y + 1, z)
                        val above2 = world.getBlockAt(x, y + 2, z)
                        if (!ground.type.isAir && above.type.isAir && above2.type.isAir) {
                            points.add(Location(world, x + 0.5, y + 1.0, z + 0.5))
                            if (points.size >= 8) return points
                        }
                    }
                }
            }
        }
        return points
    }

    private fun generateRandomParticlesAroundChest(center: Location, effect: org.bukkit.Particle) {
        repeat(25) {
            val randomXOffset = Random.nextDouble(-1.5, 1.5)
            val randomZOffset = Random.nextDouble(-1.5, 1.5)
            val randomHeightOffset = if (center.block.type == Material.CHEST) {
                Random.nextDouble(1.0, 2.5)
            } else {
                Random.nextDouble(0.3, 1.8)
            }

            val particleLoc = center.clone().add(randomXOffset, randomHeightOffset, randomZOffset)

            particleLoc.world.spawnParticle(effect, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }


    private fun claim(player: Player, location: Location) {
        var block = location.block
        block.removeMetadata("Sewers_Chest",ThePit.getInstance())
        block.type = Material.AIR
        lastClaimed = System.currentTimeMillis()

        val profile = player.getPitProfile()
        val id = randomList.random() ?: return

        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        player.sendMessage(CC.translate("&9下水道! &7你领取了下水道奖励."))

        val rewardMessage = when (id) {
            "xp" -> {
                val random = Random.nextInt(99, 8001)
                profile.experience += random.toDouble()
                profile.applyExperienceToPlayer(player)
                "- &b${random}经验"
            }

            "gold" -> {
                val random = Random.nextInt(99, 8001)
                profile.coins += random.toDouble()
                profile.grindCoins(random.toDouble())
                "- &e${random}金币"
            }

            "diamond_chestplate" -> {
                player.inventory.addItem(
                    ItemBuilder(Material.DIAMOND_CHESTPLATE).deathDrop(true).buildWithUnbreakable()
                )
                "- 钻石甲"
            }

            "diamond_leggings" -> {
                player.inventory.addItem(ItemBuilder(Material.DIAMOND_LEGGINGS).deathDrop(true).buildWithUnbreakable())
                "- 钻石裤"
            }

            "diamond_boots" -> {
                player.inventory.addItem(ItemBuilder(Material.DIAMOND_BOOTS).deathDrop(true).buildWithUnbreakable())
                "- 钻石鞋"
            }

            "rubbish" -> {
                player.inventory.addItem(ThePit.api.getMythicItemItemStack("rubbish"))
                "- &2下水道废弃物"
            }

            "mythic_leggings" -> {
                player.inventory.addItem(MythicLeggingsItem().apply { color = MythicColor.DARK_GREEN }.toItemStack())
                "- &2下水道之甲"
            }

            "speed_eggs" -> {
                player.inventory.addItem(ThePit.api.getMythicItemItemStack("speed_eggs"))
                "- &b速度蛋"
            }

            "renown" -> {
                profile.renown += 1
                "- &e声望 +1"
            }

            "milk_buckets" -> {
                player.inventory.addItem(
                    ThePit.api.getMythicItemItemStack("milk")
                )
                "- &f牛奶"
            }

            else -> {
                if (ThePit.api.getMythicItemItemStack(id).type != Material.AIR) {
                    val item = ThePit.api.getMythicItemItemStack(id)
                    player.inventory.addItem(item)
                    "- ${item.itemMeta.displayName}"
                }
                return
            }
        }
        player.sendMultiMessage("&7获得奖励: /s&7$rewardMessage")
    }
}
