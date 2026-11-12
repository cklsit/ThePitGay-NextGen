package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 23. 摇臂 */
class HammerEnchant023 : HammerEnchantBase(
    "摇臂",
    "hammer_023",
    EnchantmentRarity.NORMAL,
    listOf(-0.3, -0.5, -0.7),
    "&7命中后短暂缩短重锤攻击间隔，不叠加",
    0.0,
    0,
    HammerEffect.SPECIAL
)