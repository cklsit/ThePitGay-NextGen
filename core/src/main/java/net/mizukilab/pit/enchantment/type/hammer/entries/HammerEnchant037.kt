package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 37. 铆合 */
class HammerEnchant037 : HammerEnchantBase(
    "铆合",
    "hammer_037",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7命中后短暂提升自身护甲值",
    0.0,
    0,
    HammerEffect.SPECIAL
)
