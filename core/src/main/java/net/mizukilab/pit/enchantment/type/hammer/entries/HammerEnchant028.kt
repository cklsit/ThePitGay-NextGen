package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 28. 覆砸 */
class HammerEnchant028 : HammerEnchantBase(
    "覆砸",
    "hammer_028",
    EnchantmentRarity.NORMAL,
    listOf(7.0, 10.0, 13.0),
    "&7对处于击退或倒地状态的目标增伤",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)