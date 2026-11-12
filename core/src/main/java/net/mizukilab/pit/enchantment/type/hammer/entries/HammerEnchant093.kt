package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 93. 融雪 */
class HammerEnchant093 : HammerEnchantBase(
    "融雪",
    "hammer_093",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 1.5, 2.0),
    "&7命中清除自身减速效果并短暂提速",
    0.0,
    0,
    HammerEffect.SPECIAL
)