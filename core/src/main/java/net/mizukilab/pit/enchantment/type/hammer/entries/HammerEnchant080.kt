package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 80. 铸魂 */
class HammerEnchant080 : HammerEnchantBase(
    "铸魂",
    "hammer_080",
    EnchantmentRarity.RARE,
    listOf(1.0, 2.0, 3.0),
    "&7击杀玩家后回复 live，冷却 20 秒",
    20.0,
    0
)
