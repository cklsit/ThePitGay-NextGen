package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 43. 回炉 */
class HammerEnchant043 : HammerEnchantBase(
    "回炉",
    "hammer_043",
    EnchantmentRarity.NORMAL,
    listOf(8.0, 1.0, 1.0),
    "&7每 8 次命中回复 live，仅在上限未满时生效",
    0.0,
    0,
    HammerEffect.SPECIAL
)
