package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 44. 锭壳 */
class HammerEnchant044 : HammerEnchantBase(
    "锭壳",
    "hammer_044",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7获得一次性护盾值，耗尽后 12 秒重建",
    0.0,
    0,
    HammerEffect.SPECIAL
)