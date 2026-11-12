package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 27. 楔入 */
class HammerEnchant027 : HammerEnchantBase(
    "楔入",
    "hammer_027",
    EnchantmentRarity.NORMAL,
    listOf(0.8, 1.2, 1.6),
    "&7命中短暂禁止目标冲刺，冷却 7 秒",
    7.0,
    1,
    HammerEffect.SPECIAL
)