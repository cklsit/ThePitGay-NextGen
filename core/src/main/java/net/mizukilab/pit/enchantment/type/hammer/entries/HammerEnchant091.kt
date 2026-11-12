package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 91. 惊蛰砧 */
class HammerEnchant091 : HammerEnchantBase(
    "惊蛰砧",
    "hammer_091",
    EnchantmentRarity.SEASONAL,
    listOf(8.0, 12.0, 16.0),
    "&7命中后 3 秒内，目标受到的击退提高",
    0.0,
    0,
    HammerEffect.SPECIAL
)