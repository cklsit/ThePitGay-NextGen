package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 51. 缓锤 */
class HammerEnchant051 : HammerEnchantBase(
    "缓锤",
    "hammer_051",
    EnchantmentRarity.NORMAL,
    listOf(-4.0, -6.0, -8.0),
    "&7降低攻速换取每次命中 +6% / +9% / +12% 伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)