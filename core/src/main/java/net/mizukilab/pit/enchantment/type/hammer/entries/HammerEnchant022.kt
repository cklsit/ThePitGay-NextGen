package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 22. 砧座 */
class HammerEnchant022 : HammerEnchantBase(
    "砧座",
    "hammer_022",
    EnchantmentRarity.NORMAL,
    listOf(15.0, 20.0, 25.0),
    "&7静止且未出手 1 秒后，抵消下一次所受伤害的一部分",
    0.0,
    0,
    HammerEffect.DAMAGE_REDUCTION
)
