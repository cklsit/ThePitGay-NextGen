package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 33. 碾轮 */
class HammerEnchant033 : HammerEnchantBase(
    "碾轮",
    "hammer_033",
    EnchantmentRarity.NORMAL,
    listOf(0.5, 0.75, 1.0),
    "&7对同一目标连续 3 次命中后追加一次小额范围伤害",
    0.0,
    0
)
