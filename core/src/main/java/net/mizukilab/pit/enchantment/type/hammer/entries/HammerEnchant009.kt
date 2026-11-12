package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 9. 余温 */
class HammerEnchant009 : HammerEnchantBase(
    "余温",
    "hammer_009",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.75, 1.0),
    "&7击杀后 3 秒内持续回复少量生命值",
    0.0,
    0,
    HammerEffect.HEAL
)