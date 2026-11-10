package net.mizukilab.pit.enchantment.type.normal

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.event.PlayerOnly
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/25 15:11
 */

@ArmorOnly
class ElectrolytesEnchant : AbstractEnchantment(), IPlayerKilledEntity {
    companion object {
        private const val TICKS_PER_SECOND = 20
        private const val EXTEND_SECONDS_PER_LEVEL = 2
        private const val MAX_DURATION_BASE_LEVEL = 2
        private const val MAX_DURATION_SECONDS_MULTIPLIER = 6
    }

    override fun getEnchantName(): String {
        return "电解质"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "electrolytes_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return ("&7击杀时如自身存在 &b速度 &7效果,延长效果时间 &e" + (enchantLevel * 2) + " 秒"
                + "/s&7(如效果等级大于II则延长时间减半,上限" + ((enchantLevel + 2) * 6) + "秒)")
    }

    @PlayerOnly
    override fun handlePlayerKilled(
        enchantLevel: Int,
        myself: Player,
        target: Entity?,
        coins: AtomicDouble?,
        experience: AtomicDouble?
    ) {
        myself.activePotionEffects.stream()
            .filter { effect: PotionEffect -> effect.type === PotionEffectType.SPEED }
            .findFirst()
            .ifPresent { potionEffect: PotionEffect ->
                val maxDuration = maxDurationTicks(enchantLevel)
                val extendedDuration = potionEffect.duration + extensionTicks(enchantLevel, potionEffect.amplifier)
                val duration = minOf(maxDuration, extendedDuration)
                if (duration <= potionEffect.duration) {
                    return@ifPresent
                }

                myself.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SPEED,
                        duration,
                        potionEffect.amplifier
                    ), true
                )
            }
    }

    private fun extensionTicks(enchantLevel: Int, amplifier: Int): Int {
        var extension = enchantLevel * EXTEND_SECONDS_PER_LEVEL * TICKS_PER_SECOND
        if (amplifier > 1) {
            extension /= 2
        }
        return extension
    }

    private fun maxDurationTicks(enchantLevel: Int): Int {
        return (enchantLevel + MAX_DURATION_BASE_LEVEL) *
                MAX_DURATION_SECONDS_MULTIPLIER *
                TICKS_PER_SECOND
    }
}
