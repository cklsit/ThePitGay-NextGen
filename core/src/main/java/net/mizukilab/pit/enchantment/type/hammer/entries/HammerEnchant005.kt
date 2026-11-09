package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 5. 沉肩 */
class HammerEnchant005 : HammerEnchantBase(
    "沉肩",
    "hammer_005",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 6.0, 8.0),
    "&7站定超过 0.8 秒时减伤，移动即失效",
    0.0,
    0
)
