package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 7. 铁屑 */
class HammerEnchant007 : HammerEnchantBase(
    "铁屑",
    "hammer_007",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.75, 1.0),
    "&7命中时向 2 格内其他目标溅射小额伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)