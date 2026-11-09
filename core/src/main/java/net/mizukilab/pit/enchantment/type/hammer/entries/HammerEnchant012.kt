package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 12. 桩基 */
class HammerEnchant012 : HammerEnchantBase(
    "桩基",
    "hammer_012",
    EnchantmentRarity.NORMAL,
    listOf(30.0, 50.0, 70.0),
    "&7站定 1 秒后获得击退抗性，被位移打断即失效",
    0.0,
    0
)
