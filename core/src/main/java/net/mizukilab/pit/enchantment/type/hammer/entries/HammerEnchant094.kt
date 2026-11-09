package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 94. 骤雨 */
class HammerEnchant094 : HammerEnchantBase(
    "骤雨",
    "hammer_094",
    EnchantmentRarity.SEASONAL,
    listOf(2.0, 3.0, 4.0),
    "&7落点生成短暂雨幕，雨幕内自身的下一次命中附带小额范围冲击",
    0.0,
    1
)
