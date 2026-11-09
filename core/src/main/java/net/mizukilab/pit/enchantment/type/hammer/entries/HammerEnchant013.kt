package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 13. 闷响 */
class HammerEnchant013 : HammerEnchantBase(
    "闷响",
    "hammer_013",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 1.5, 2.0),
    "&7命中使目标短暂减速，同一目标 5 秒内只生效一次",
    0.0,
    0
)
