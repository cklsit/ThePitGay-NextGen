package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 61. 沉锤 */
class HammerEnchant061 : HammerEnchantBase(
    "沉锤",
    "hammer_061",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 6.0, 8.0),
    "&7对生命上限高于自身的目标增伤",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
