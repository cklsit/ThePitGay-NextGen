package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 67. 铁腥 */
class HammerEnchant067 : HammerEnchantBase(
    "铁腥",
    "hammer_067",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中使目标短暂无法使用治疗类物品",
    0.0,
    0,
    HammerEffect.SPECIAL
)