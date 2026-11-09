package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 63. 压舱 */
class HammerEnchant063 : HammerEnchantBase(
    "压舱",
    "hammer_063",
    EnchantmentRarity.NORMAL,
    listOf(-5.0, -8.0, -11.0),
    "&7降低自身受到的击退，移动时效果减半",
    0.0,
    0
)
