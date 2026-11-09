package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 60. 双层锭 */
class HammerEnchant060 : HammerEnchantBase(
    "双层锭",
    "hammer_060",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7连续命中叠加少量减伤，受击清空一半",
    0.0,
    0
)
