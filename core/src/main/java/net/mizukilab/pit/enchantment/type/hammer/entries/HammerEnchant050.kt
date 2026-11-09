package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 50. 秤星 */
class HammerEnchant050 : HammerEnchantBase(
    "秤星",
    "hammer_050",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7命中后以 ActionBar 显示自身 live/maxLive 比例提示",
    0.0,
    0
)
