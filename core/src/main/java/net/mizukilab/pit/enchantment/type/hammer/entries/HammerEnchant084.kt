package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 84. 断层 */
class HammerEnchant084 : HammerEnchantBase(
    "断层",
    "hammer_084",
    EnchantmentRarity.RARE,
    listOf(1.5, 2.0, 2.5),
    "&7重击在目标身后生成一道短暂阻挡通行的逻辑断层，不放真方块",
    0.0,
    2,
    HammerEffect.SPECIAL
)
