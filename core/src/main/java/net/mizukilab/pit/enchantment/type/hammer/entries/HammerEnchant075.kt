package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 75. 引锤 */
class HammerEnchant075 : HammerEnchantBase(
    "引锤",
    "hammer_075",
    EnchantmentRarity.RARE,
    listOf(2.0, 3.0, 4.0),
    "&7命中把周围敌人向落点聚拢并短暂禁止位移，冷却 10 秒",
    10.0,
    1,
    HammerEffect.SPECIAL
)