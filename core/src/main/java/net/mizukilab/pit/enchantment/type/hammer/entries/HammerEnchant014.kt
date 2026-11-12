package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 14. 钝锋 */
class HammerEnchant014 : HammerEnchantBase(
    "钝锋",
    "hammer_014",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 8.0, 11.0),
    "&7对装备护甲的目标增伤，对无护甲目标无效",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)