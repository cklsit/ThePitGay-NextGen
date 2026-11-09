package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 1. 落砧 */
class HammerEnchant001 : HammerEnchantBase(
    "落砧",
    "hammer_001",
    EnchantmentRarity.NORMAL,
    listOf(6.0, 9.0, 12.0),
    "&7每 4 次命中后，下一次重击附带一次小范围冲击伤害，冲击本身不再触发本条",
    0.0,
    0,
    HammerEffect.SPECIAL
)
