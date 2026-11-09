package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 30. 轨痕 */
class HammerEnchant030 : HammerEnchantBase(
    "轨痕",
    "hammer_030",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.8, 1.2),
    "&7冲刺后首次命中附带一小段前冲",
    0.0,
    0,
    HammerEffect.SPECIAL
)
