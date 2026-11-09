package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 62. 锈痕 */
class HammerEnchant062 : HammerEnchantBase(
    "锈痕",
    "hammer_062",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中使目标的装备增益短暂失效，不影响附魔",
    0.0,
    0,
    HammerEffect.SPECIAL
)
