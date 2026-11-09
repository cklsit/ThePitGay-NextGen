package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 8. 冷锻 */
class HammerEnchant008 : HammerEnchantBase(
    "冷锻",
    "hammer_008",
    EnchantmentRarity.NORMAL,
    listOf(3.0, 5.0, 7.0),
    "&7常驻小幅减伤，不随血量提升",
    0.0,
    0
)
