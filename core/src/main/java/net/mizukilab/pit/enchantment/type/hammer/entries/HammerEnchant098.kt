package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 98. 霜砧 */
class HammerEnchant098 : HammerEnchantBase(
    "霜砧",
    "hammer_098",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 1.5, 2.0),
    "&7命中在目标脚下留下霜痕，短暂降低其攻击速度",
    0.0,
    0
)
