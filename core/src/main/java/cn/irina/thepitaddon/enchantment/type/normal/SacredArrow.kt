package cn.irina.thepitaddon.enchantment.type.normal

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

import java.util.concurrent.atomic.AtomicBoolean

@BowOnly
class SacredArrow : AbstractEnchantment(), IPlayerShootEntity {
    override fun getEnchantName(): String {
        return "圣净之矢"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "interdiction"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7射出的箭矢命中玩家时可对其施加 &b净化 &7效果, 同时额外造成 &c" + enchantLevel * 0.5 + "❤ &7的伤害"
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity?,
        damage: Double,
        finalDamage: AtomicDouble?,
        boostDamage: AtomicDouble?,
        cancel: AtomicBoolean?
    ) {
        if (target !is Player) return

        val player = target
        PlayerUtil.damage(player, PlayerUtil.DamageType.NORMAL, enchantLevel.toDouble(), true)
        if (player.hasPotionEffect(PotionEffectType.SPEED)) player.removePotionEffect(PotionEffectType.SPEED)
        if (player.hasPotionEffect(PotionEffectType.JUMP)) player.removePotionEffect(PotionEffectType.JUMP)
    }
}
