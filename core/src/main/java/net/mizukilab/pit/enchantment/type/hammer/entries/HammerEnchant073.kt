package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 73. 千锻 */
class HammerEnchant073 : HammerEnchantBase(
    "千锻",
    "hammer_073",
    EnchantmentRarity.RARE,
    listOf(3.0, 4.0, 5.0),
    "&7连续命中且未切换目标时，每次回复 0.25 ❤ 并小幅提升攻速，切换目标清空",
    0.0,
    0,
    HammerEffect.HEAL
)