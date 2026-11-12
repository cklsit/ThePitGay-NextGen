package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 20. 熔渣 */
class HammerEnchant020 : HammerEnchantBase(
    "熔渣",
    "hammer_020",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7落点留下熔渣区域，区域内目标移速降低，冷却 8 秒",
    8.0,
    1,
    HammerEffect.SPECIAL
)