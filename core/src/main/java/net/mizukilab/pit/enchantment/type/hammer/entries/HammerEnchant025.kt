package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 25. 断纹 */
class HammerEnchant025 : HammerEnchantBase(
    "断纹",
    "hammer_025",
    EnchantmentRarity.NORMAL,
    listOf(6.0, 9.0, 12.0),
    "&7目标生命值低于一半时增伤",
    0.0,
    0
)
