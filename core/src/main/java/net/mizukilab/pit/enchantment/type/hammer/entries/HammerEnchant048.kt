package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 48. 砧裂 */
class HammerEnchant048 : HammerEnchantBase(
    "砧裂",
    "hammer_048",
    EnchantmentRarity.NORMAL,
    listOf(6.0, 9.0, 12.0),
    "&7live 低于 maxLive 一半时增伤",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)
