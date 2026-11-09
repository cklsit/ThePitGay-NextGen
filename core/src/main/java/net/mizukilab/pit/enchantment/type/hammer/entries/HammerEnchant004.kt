package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 4. 回摆 */
class HammerEnchant004 : HammerEnchantBase(
    "回摆",
    "hammer_004",
    EnchantmentRarity.NORMAL,
    listOf(8.0, 12.0, 16.0),
    "&7挥空后的下一次命中增伤，命中即重置",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
