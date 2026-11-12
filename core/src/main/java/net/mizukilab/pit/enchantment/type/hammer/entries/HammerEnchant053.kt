package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 53. 砧垫 */
class HammerEnchant053 : HammerEnchantBase(
    "砧垫",
    "hammer_053",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7受到致命伤时保留 1 点生命值并消耗等量 live，冷却 60 秒",
    60.0,
    0,
    HammerEffect.SPECIAL
)