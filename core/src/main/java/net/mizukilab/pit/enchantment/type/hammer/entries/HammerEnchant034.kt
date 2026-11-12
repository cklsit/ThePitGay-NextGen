package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 34. 退火 */
class HammerEnchant034 : HammerEnchantBase(
    "退火",
    "hammer_034",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7受到暴击后短暂降低后续受到的伤害，冷却 10 秒",
    10.0,
    0,
    HammerEffect.DAMAGE_REDUCTION
)