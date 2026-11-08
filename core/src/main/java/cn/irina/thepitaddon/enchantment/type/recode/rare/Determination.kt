package cn.irina.thepitaddon.enchantment.type.recode.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.RomanUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.utils.TimeUtil.formatTotalSeconds
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ArmorOnly
class Determination : AbstractEnchantment(),  IPlayerDamaged, IActionDisplayEnchant {
    private val cooldown = HashMap<UUID, Cooldown>()

    override fun getEnchantName(): String {
        return "决心"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "FightOrDie"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7受击时, 若自身血量低于最大生命值的 &c${10 + (enchantLevel * 10)}%❤ &7, 则获得效果: /s" +
                "  &f▶ &3抗性提升 ${RomanUtil.convert(enchantLevel)} &7(00:04) /s" +
                "  &f▶ &b速度 I &7(${formatTotalSeconds(if (enchantLevel >= 3) 6 else 4)}) /s" +
                "&7同时, 恢复自身血量 &c${(enchantLevel + 1).toDouble()}❤ /s" +
                "&7此附魔每 &f10 &7秒触发一次"
    }

    override fun handlePlayerDamaged(
        enchantLevel: Int,
        victim: Player,
        attack: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (victim.health >= victim.maxHealth * ((10 + (enchantLevel * 10)) * 0.01) || !cooldown.getOrDefault(victim.uniqueId, Cooldown(0L)).hasExpired()) return
        cooldown[victim.uniqueId] = Cooldown(10L, TimeUnit.SECONDS)

        if (victim.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) victim.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
        if (victim.hasPotionEffect(PotionEffectType.SPEED)) victim.removePotionEffect(PotionEffectType.SPEED)

        victim.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 4, enchantLevel - 1, false, true))
        victim.addPotionEffect(PotionEffect(PotionEffectType.SPEED, (if (enchantLevel >= 3) 20 * 6 else 20 * 4), 0, false, true))

        PlayerUtil.heal(victim, (if (enchantLevel >= 2) 6 else 4).toDouble())
    }

    override fun getText(i: Int, player: Player): String {
        return getCooldownActionText(cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }
}
