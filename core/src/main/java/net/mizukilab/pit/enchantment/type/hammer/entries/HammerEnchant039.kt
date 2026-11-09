package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 39. 碎砂 */
class HammerEnchant039 : HammerEnchantBase(
    "碎砂",
    "hammer_039",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.5, 2.0),
    "&7命中使目标短暂无法获得新的增益效果",
    0.0,
    0
)
