package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 71. 陨砧 */
class HammerEnchant071 : HammerEnchantBase(
    "陨砧",
    "hammer_071",
    EnchantmentRarity.RARE,
    listOf(14.0, 20.0, 26.0),
    "&7蓄力 1.5 秒后落锤造成范围伤害并附加 1 秒眩晕，冷却 12 秒",
    12.0,
    2,
    HammerEffect.SPECIAL
)
