package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 72. 地脉 */
class HammerEnchant072 : HammerEnchantBase(
    "地脉",
    "hammer_072",
    EnchantmentRarity.RARE,
    listOf(3.0, 4.0, 5.0),
    "&7重击在地面留下持续 4 秒的裂隙（粒子 + 逻辑区域），裂隙内目标受到的伤害提高",
    0.0,
    1,
    HammerEffect.SPECIAL
)