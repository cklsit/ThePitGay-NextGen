package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 42. 毛边 */
class HammerEnchant042 : HammerEnchantBase(
    "毛边",
    "hammer_042",
    EnchantmentRarity.NORMAL,
    listOf(3.0, 5.0, 7.0),
    "&7对已带减益的目标增伤，每多一种 +1%，上限 +3%",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
