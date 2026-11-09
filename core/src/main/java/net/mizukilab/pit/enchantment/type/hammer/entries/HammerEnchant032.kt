package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 32. 坠点 */
class HammerEnchant032 : HammerEnchantBase(
    "坠点",
    "hammer_032",
    EnchantmentRarity.NORMAL,
    listOf(8.0, 12.0, 16.0),
    "&7从高处落下的首次命中增伤（受猛击 +50% 封顶约束）",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
