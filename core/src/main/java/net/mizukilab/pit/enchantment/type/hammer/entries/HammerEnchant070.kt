package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 70. 老锤 */
class HammerEnchant070 : HammerEnchantBase(
    "老锤",
    "hammer_070",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 4.0, 6.0),
    "&7live 越低伤害越高，按每 10% 上限一档线性计算",
    0.0,
    0
)
