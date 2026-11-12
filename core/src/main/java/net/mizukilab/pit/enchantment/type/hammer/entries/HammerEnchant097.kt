package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 97. 收锭 */
class HammerEnchant097 : HammerEnchantBase(
    "收锭",
    "hammer_097",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 2.0, 3.0),
    "&7击杀时按目标连杀数额外获得少量经验，且本次击杀不消耗 live",
    0.0,
    0,
    HammerEffect.SPECIAL
)