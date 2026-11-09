package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 99. 落叶 */
class HammerEnchant099 : HammerEnchantBase(
    "落叶",
    "hammer_099",
    EnchantmentRarity.SEASONAL,
    listOf(0.5, 0.75, 1.0),
    "&7命中处于减速状态的目标时额外造成一次小额范围伤害，冷却 6 秒",
    6.0,
    0,
    HammerEffect.SPECIAL
)
