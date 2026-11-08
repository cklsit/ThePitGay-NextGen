package cn.irina.thepitaddon.enchantment.type.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@WeaponOnly
class JudgmentStrike : AbstractEnchantment(),  IAttackEntity, IActionDisplayEnchant {
    private val cooldown = HashMap<UUID, Cooldown>()

    override fun getEnchantName(): String {
        return "裁决之击"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "judgment_strike"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7下一次对目标造成的伤害 &c+" + ((enchantLevel * 40) + 30) + "% &7(" + (if (enchantLevel >= 3) 15 else 20) + "s冷却)"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (target !is Player) return

        if (!cooldown.getOrDefault(attacker.uniqueId, Cooldown(0L)).hasExpired()) return

        cooldown[attacker.uniqueId] =
            Cooldown((if (enchantLevel >= 3) 15 else 20).toLong(), TimeUnit.SECONDS)
        boostDamage.getAndAdd((enchantLevel * 0.4) + 0.3)
    }

    override fun getText(i: Int, player: Player): String {
        return getCooldownActionText(cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }
}
