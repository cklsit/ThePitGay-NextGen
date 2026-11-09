package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 96. 潮鸣 */
class HammerEnchant096 : HammerEnchantBase(
    "潮鸣",
    "hammer_096",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中 2 名以上目标时短暂提升自身移速",
    0.0,
    0,
    HammerEffect.SPECIAL
)
