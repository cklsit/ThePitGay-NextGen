package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 57. 落点尺 */
class HammerEnchant057 : HammerEnchantBase(
    "落点尺",
    "hammer_057",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7以自身可见的粒子圆环显示重击落点范围",
    0.0,
    0,
    HammerEffect.SPECIAL
)