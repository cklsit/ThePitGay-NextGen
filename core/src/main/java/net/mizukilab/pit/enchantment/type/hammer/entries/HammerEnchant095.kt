package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 95. 灼砧 */
class HammerEnchant095 : HammerEnchantBase(
    "灼砧",
    "hammer_095",
    EnchantmentRarity.SEASONAL,
    listOf(6.0, 9.0, 12.0),
    "&7生命值高于 60% 时，命中后短暂提升攻速，脱战即失效",
    0.0,
    0
)
