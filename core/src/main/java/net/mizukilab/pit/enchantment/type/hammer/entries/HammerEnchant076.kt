package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 76. 反锻 */
class HammerEnchant076 : HammerEnchantBase(
    "反锻",
    "hammer_076",
    EnchantmentRarity.RARE,
    listOf(20.0, 30.0, 40.0),
    "&7受到近战伤害时把一部分以冲击形式返还给攻击者，冷却 5 秒",
    5.0,
    0,
    HammerEffect.SPECIAL
)
