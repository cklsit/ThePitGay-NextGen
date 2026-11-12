package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 36. 击桩 */
class HammerEnchant036 : HammerEnchantBase(
    "击桩",
    "hammer_036",
    EnchantmentRarity.NORMAL,
    listOf(6.0, 9.0, 12.0),
    "&7对 1 秒内未移动的目标增伤",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)