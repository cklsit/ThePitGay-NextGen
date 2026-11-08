package cn.irina.thepitaddon.enchantment.type.normal

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

@WeaponOnly
@ArmorOnly
class Athena : AbstractEnchantment(), IPlayerKilledEntity {
    override fun getEnchantName(): String {
        return "雅典娜"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "athena"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7击杀获得 &b+" + (enchantLevel * 4) + " 经验值"
    }

    override fun handlePlayerKilled(
        enchantLevel: Int,
        killer: Player,
        entity: Entity,
        coins: AtomicDouble,
        experience: AtomicDouble
    ) {
        experience.set(experience.get() + (enchantLevel * 4))
    }
}