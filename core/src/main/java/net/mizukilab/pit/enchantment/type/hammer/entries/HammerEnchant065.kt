package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 65. 短砧 */
class HammerEnchant065 : HammerEnchantBase(
    "短砧",
    "hammer_065",
    EnchantmentRarity.NORMAL,
    listOf(-0.2, -0.35, -0.5),
    "&7缩短重锤前摇，不改变伤害（并入攻击冷却）",
    0.0,
    0,
    HammerEffect.SPECIAL
)
