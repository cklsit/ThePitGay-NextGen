package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 6. 惯性 */
class HammerEnchant006 : HammerEnchantBase(
    "惯性",
    "hammer_006",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 7.0, 10.0),
    "&7移动中命中增伤，静止命中无效",
    0.0,
    0
)
