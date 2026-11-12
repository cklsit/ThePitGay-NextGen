package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 24. 锭心 */
class HammerEnchant024 : HammerEnchantBase(
    "锭心",
    "hammer_024",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 8.0, 11.0),
    "&7`live = maxLive` 时增伤，`live` 低于上限即失效",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)