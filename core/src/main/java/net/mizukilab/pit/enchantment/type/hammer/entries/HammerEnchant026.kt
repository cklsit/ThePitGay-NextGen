package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 26. 铁律 */
class HammerEnchant026 : HammerEnchantBase(
    "铁律",
    "hammer_026",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 6.0, 8.0),
    "&7自身处于减速或控制时受到的伤害降低",
    0.0,
    0,
    HammerEffect.DAMAGE_REDUCTION
)