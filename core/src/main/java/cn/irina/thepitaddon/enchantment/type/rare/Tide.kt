package cn.irina.thepitaddon.enchantment.type.rare

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/*
 * @Author Irina
 * @Date 2025/6/6 23:55
 */

@WeaponOnly
@ArmorOnly

class Tide : AbstractEnchantment(), IPlayerKilledEntity {
    override fun getEnchantName(): String {
        return "潮汐"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "tide"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(level: Int): String {
        return "击杀获得的经验 &b+${30 + (level * 20)}%"
    }

    override fun handlePlayerKilled(
        level: Int,
        killer: Player,
        entity: Entity,
        coin: AtomicDouble,
        exp: AtomicDouble,
    ) {
        exp.set(exp.get() * (1.3 + 0.2 * level))
    }
}