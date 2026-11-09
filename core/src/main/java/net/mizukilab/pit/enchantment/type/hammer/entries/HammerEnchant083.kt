package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 83. 熔芯 */
class HammerEnchant083 : HammerEnchantBase(
    "熔芯",
    "hammer_083",
    EnchantmentRarity.RARE,
    listOf(3.0, 4.0, 5.0),
    "&7命中使目标身上的持续伤害效果立即结算一次",
    0.0,
    1,
    HammerEffect.SPECIAL
)
