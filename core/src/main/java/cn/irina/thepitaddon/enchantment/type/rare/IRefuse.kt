package cn.irina.thepitaddon.enchantment.type.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.cooldown.Cooldown

/**
 * 稀有附魔「我拒绝」：禁用你在挂机池中的自动精通功能。
 * 作用于神话之甲（护腿），可在神话之井产出。
 */
@ArmorOnly
class IRefuse : AbstractEnchantment() {

    override fun getEnchantName(): String {
        return "我拒绝"
    }

    override fun getMaxEnchantLevel(): Int {
        return 1
    }

    override fun getNbtName(): String {
        return "i_refuse"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(level: Int): String {
        return "&7禁用你在挂机池中的自动精通功能"
    }
}
