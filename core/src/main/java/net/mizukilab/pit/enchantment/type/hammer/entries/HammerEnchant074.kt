package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 74. 崩砧 */
class HammerEnchant074 : HammerEnchantBase(
    "崩砧",
    "hammer_074",
    EnchantmentRarity.RARE,
    listOf(18.0, 24.0, 30.0),
    "&7live 归零前的最后一次命中大幅提升伤害，触发后 `live` 归零，按 §5.2 不可用",
    0.0,
    0,
    HammerEffect.SPECIAL
)
