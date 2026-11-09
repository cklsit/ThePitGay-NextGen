package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 47. 铁水 */
class HammerEnchant047 : HammerEnchantBase(
    "铁水",
    "hammer_047",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.75, 1.0),
    "&7命中处于减速区域的目标时回复少量生命值",
    0.0,
    0,
    HammerEffect.HEAL
)
