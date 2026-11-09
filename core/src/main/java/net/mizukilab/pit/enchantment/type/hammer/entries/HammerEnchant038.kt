package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 38. 承力 */
class HammerEnchant038 : HammerEnchantBase(
    "承力",
    "hammer_038",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 8.0, 11.0),
    "&7受到伤害后 2 秒内的下一次命中增伤",
    0.0,
    0
)
