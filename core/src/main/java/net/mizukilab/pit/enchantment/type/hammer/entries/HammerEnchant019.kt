package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 19. 砂型 */
class HammerEnchant019 : HammerEnchantBase(
    "砂型",
    "hammer_019",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.5, 2.0),
    "&7命中短暂压制目标的暴击几率",
    0.0,
    0,
    HammerEffect.SPECIAL
)
