package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 29. 地鸣 */
class HammerEnchant029 : HammerEnchantBase(
    "地鸣",
    "hammer_029",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 2.5, 3.0),
    "&7重击命中时发出冲击波，只造成位移不造成伤害，冷却 6 秒",
    6.0,
    1,
    HammerEffect.SPECIAL
)