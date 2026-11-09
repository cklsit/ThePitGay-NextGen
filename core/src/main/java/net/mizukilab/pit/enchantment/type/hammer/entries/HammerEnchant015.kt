package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 15. 秤砣 */
class HammerEnchant015 : HammerEnchantBase(
    "秤砣",
    "hammer_015",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 6.0, 8.0),
    "&7目标当前生命值高于自身时增伤",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
