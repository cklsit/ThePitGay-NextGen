package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 59. 折返 */
class HammerEnchant059 : HammerEnchantBase(
    "折返",
    "hammer_059",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.0, 2.0),
    "&7被击退后 2 秒内首次命中附带一次反推",
    0.0,
    0
)
