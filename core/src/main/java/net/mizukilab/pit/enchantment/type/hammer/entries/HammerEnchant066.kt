package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 66. 断锤 */
class HammerEnchant066 : HammerEnchantBase(
    "断锤",
    "hammer_066",
    EnchantmentRarity.NORMAL,
    listOf(8.0, 12.0, 16.0),
    "&7目标处于无法移动状态时增伤",
    0.0,
    1,
    HammerEffect.DAMAGE_PCT
)