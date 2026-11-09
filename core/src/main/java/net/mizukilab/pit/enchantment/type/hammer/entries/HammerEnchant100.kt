package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 100. 归砧 */
class HammerEnchant100 : HammerEnchantBase(
    "归砧",
    "hammer_100",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 2.0, 3.0),
    "&7连杀结束时额外保留 live，且下一次进入神话之井的首击附带一层锤印",
    0.0,
    0,
    HammerEffect.SPECIAL
)
