package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 79. 碎星 */
class HammerEnchant079 : HammerEnchantBase(
    "碎星",
    "hammer_079",
    EnchantmentRarity.RARE,
    listOf(30.0, 40.0, 50.0),
    "&7命中时把目标当前护盾值的一部分转为额外伤害，冷却 8 秒",
    8.0,
    0,
    HammerEffect.SPECIAL
)