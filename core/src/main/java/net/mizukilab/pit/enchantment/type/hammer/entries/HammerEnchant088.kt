package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 88. 压铸 */
class HammerEnchant088 : HammerEnchantBase(
    "压铸",
    "hammer_088",
    EnchantmentRarity.RARE,
    listOf(10.0, 15.0, 20.0),
    "&7对带有 3 种及以上减益的目标增伤，并短暂禁用其增益",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)