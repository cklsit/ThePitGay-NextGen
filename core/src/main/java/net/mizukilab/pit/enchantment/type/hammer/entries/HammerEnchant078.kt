package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 78. 定盘 */
class HammerEnchant078 : HammerEnchantBase(
    "定盘",
    "hammer_078",
    EnchantmentRarity.RARE,
    listOf(1.0, 2.0, 3.0),
    "&7命中短暂锁定目标的物品栏切换，并附带小额真实伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)