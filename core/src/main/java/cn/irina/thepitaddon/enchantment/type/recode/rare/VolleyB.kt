package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.item.ItemBuilder
import cn.irina.thepitaddon.Main
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.scheduler.BukkitRunnable

import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.TimeUnit

@BowOnly
class VolleyB : AbstractEnchantment(),  Listener {
    private val cooldown = HashMap<UUID, Cooldown>()
    private val arrowBuilder: ItemBuilder = ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false)

    init {
    }

    override fun getEnchantName(): String {
        return "连续射击"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "volley_enchant_B"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "射箭时同时射出 &e${enchantLevel + 2} &7支箭矢"
    }

    @EventHandler
    fun onInteract(event: EntityShootBowEvent) {
        if (event.entity !is Player) return

        val player = event.entity as Player

        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return

        val itemInHand = player.itemInHand
        if (itemInHand == null || itemInHand.type != Material.BOW) return

        val level = ThePit.getApi().getItemEnchantLevel(itemInHand, this.nbtName)
        if (level <= 0) return

        if (!cooldown.getOrDefault(player.uniqueId, Cooldown(0L)).hasExpired()) return
        cooldown[player.uniqueId] = Cooldown(((level + 1) * 100L) + 14L, TimeUnit.MILLISECONDS)
        event.isCancelled = true
        val entityPlayer = player

        val value: Int = (event.force * 20).toInt().coerceIn(1, 20)

        player.inventory.addItem(arrowBuilder.amount(level + 1).build())

        object : BukkitRunnable() {
            var tick: Int = 0
            override fun run() {
                val profile = PlayerProfile.getRawCache(player.getUniqueId())
                if (tick > level + 1 || cooldown.getOrDefault(player.getUniqueId(), Cooldown(0L))
                        .hasExpired() || !profile.isInArena
                ) {
                    cancel()
                    return
                }

                ++tick
                val arrow = player.launchProjectile(org.bukkit.entity.Arrow::class.java)
                arrow.velocity = player.location.direction.multiply(3.0 * (value / 20.0))
            }
        }.runTaskTimer(Main.instance, 0L, 2L)
    }
}