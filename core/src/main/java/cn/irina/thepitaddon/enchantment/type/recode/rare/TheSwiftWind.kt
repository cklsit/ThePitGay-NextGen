package cn.irina.thepitaddon.enchantment.type.recode.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

import java.util.concurrent.atomic.AtomicBoolean

@ArmorOnly
class TheSwiftWind : AbstractEnchantment(),  IAttackEntity {
    override fun getEnchantName(): String {
        return "迅捷之风"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "theswiftwind_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantmentLevel: Int): String {
        return "&7每拥有一级 &b速度 &7药水效果, 你的伤害将提升 &c+" + enchantmentLevel * 4 + "%"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean?
    ) {
        for (effect in player.activePotionEffects) {
            if (effect.type != PotionEffectType.SPEED) continue
            boostDamage.getAndAdd((effect.amplifier + 1) * (enchantLevel * 0.04))
            player.sendMessage(CC.translate("&bBoosted"))
            return
        }
    }
}
