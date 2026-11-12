package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 31. 收势 */
class HammerEnchant031 : HammerEnchantBase(
    "收势",
    "hammer_031",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.75, 1.0),
    "&7连续 3 次挥空后，下一次命中回复少量生命值",
    0.0,
    0,
    HammerEffect.HEAL
)