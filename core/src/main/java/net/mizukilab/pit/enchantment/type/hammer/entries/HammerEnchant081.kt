package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 81. 逆锤 */
class HammerEnchant081 : HammerEnchantBase(
    "逆锤",
    "hammer_081",
    EnchantmentRarity.RARE,
    listOf(12.0, 18.0, 24.0),
    "&7自身生命值低于 30% 时命中伤害提升，且击退方向反转",
    0.0,
    0
)
