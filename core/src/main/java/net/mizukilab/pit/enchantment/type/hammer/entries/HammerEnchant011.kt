package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 11. 夯土 */
class HammerEnchant011 : HammerEnchantBase(
    "夯土",
    "hammer_011",
    EnchantmentRarity.NORMAL,
    listOf(1.5, 2.0, 2.5),
    "&7落点产生一圈击退，冷却 6 秒",
    6.0,
    1,
    HammerEffect.SPECIAL
)
