package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 49. 补锭 */
class HammerEnchant049 : HammerEnchantBase(
    "补锭",
    "hammer_049",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.0, 2.0),
    "&7击杀非玩家目标回复 live，冷却 15 秒",
    15.0,
    0,
    HammerEffect.SPECIAL
)
