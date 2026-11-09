package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 41. 铁砧回声 */
class HammerEnchant041 : HammerEnchantBase(
    "铁砧回声",
    "hammer_041",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7命中留下回声标记，队友命中该目标时回复少量生命值",
    0.0,
    0
)
