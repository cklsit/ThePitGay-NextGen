package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 45. 欠火 */
class HammerEnchant045 : HammerEnchantBase(
    "欠火",
    "hammer_045",
    EnchantmentRarity.NORMAL,
    listOf(-5.0, -8.0, -11.0),
    "&7自身造成的伤害降低，但每次命中回复 0.25 / 0.5 / 0.75 ❤",
    0.0,
    0,
    HammerEffect.HEAL
)