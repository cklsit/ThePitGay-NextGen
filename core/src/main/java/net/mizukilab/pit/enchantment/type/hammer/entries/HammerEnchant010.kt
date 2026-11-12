package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 10. 铆钉 */
class HammerEnchant010 : HammerEnchantBase(
    "铆钉",
    "hammer_010",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7命中给目标打上铆钉标记，标记期间其受到的钝器伤害小幅提高",
    0.0,
    0,
    HammerEffect.SPECIAL
)