package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 85. 秤衡 */
class HammerEnchant085 : HammerEnchantBase(
    "秤衡",
    "hammer_085",
    EnchantmentRarity.RARE,
    listOf(10.0, 15.0, 20.0),
    "&7把本次受到伤害的一部分推迟到 4 秒内分摊结算",
    0.0,
    0,
    HammerEffect.SPECIAL
)