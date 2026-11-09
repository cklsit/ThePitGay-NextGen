package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 2. 配重 */
class HammerEnchant002 : HammerEnchantBase(
    "配重",
    "hammer_002",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 8.0, 11.0),
    "&7收锤超过 1.2 秒后的首次命中增伤，连续快挥不生效",
    0.0,
    0
)
