package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 69. 换砧 */
class HammerEnchant069 : HammerEnchantBase(
    "换砧",
    "hammer_069",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.0, 2.0),
    "&7切换手持物品后首次命中附带小额额外伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)