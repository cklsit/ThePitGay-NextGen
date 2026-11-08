package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.manager.PitManager
import cn.irina.thepitaddon.manager.PitManager.hasPitEnchant
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

/*
 * @Author ShanguanLinG
 * @Date 2025/9/12 10:10
 */

@ArmorOnly
class Heavy : AbstractEnchantment(), Listener {
    override fun getEnchantName(): String {
        return "厚重"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "heavy"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(level: Int): String {
        return "&7穿着时自身最大生命值提升 &c${level * 0.5 + 0.5}❤"
    }

    @EventHandler
    fun onExit(event: PlayerQuitEvent) {
        val profile = PlayerProfile.getRawCache(event.player.uniqueId)
        profile?.extraMaxHealth?.remove("heavy")
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val leggings = player.inventory.leggings
        if (leggings != null && hasPitEnchant(leggings, "heavy")) {
            val profile = PlayerProfile.getRawCache(event.player.uniqueId) ?: return
            val item = player.inventory.leggings
            if (!hasPitEnchant(item, "heavy")) return
            profile.extraMaxHealth["heavy"] = (
                    PitManager.getPitEnchantLevel(
                        item,
                        "heavy"
                    ) * 0.5 + 0.5) * 2
            player.maxHealth = profile.maxHealth
            return
        }
        val profile = PlayerProfile.getRawCache(event.player.uniqueId) ?: return

        if (!profile.extraMaxHealth.containsKey("heavy")) return
        profile.extraMaxHealth.remove("heavy")
        player.maxHealth = profile.maxHealth
    }
}