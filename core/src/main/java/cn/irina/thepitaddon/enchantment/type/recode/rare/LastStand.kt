package cn.irina.thepitaddon.enchantment.type.recode.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
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
class LastStand : AbstractEnchantment(),  IPlayerDamaged, IActionDisplayEnchant {
    private val cooldown = HashMap<UUID, Cooldown>()


    override fun getEnchantName(): String {
        return "背水一战"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "last_stand"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7若受击时血量低于 &c${(3 + enchantLevel).toDouble()}❤ &7时, 立刻获得效果 &c生命恢复 " + RomanUtil.convert(enchantLevel + 2) + " &f(" + formatTotalSeconds(enchantLevel + 1) + ") &7(10s冷却)"
    }

    override fun handlePlayerDamaged(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (player.health > 10 || !cooldown.getOrDefault(player.uniqueId, Cooldown(0L)).hasExpired()) return
        cooldown[player.uniqueId] = Cooldown(8L, TimeUnit.SECONDS)

        if (player.hasPotionEffect(PotionEffectType.REGENERATION)) player.removePotionEffect(PotionEffectType.REGENERATION)
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.REGENERATION,
                (enchantLevel + 2) * 20,
                enchantLevel,
                false,
                true
            )
        )
    }

    override fun getText(i: Int, player: Player): String {
        return getCooldownActionText(cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }

}
