package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 56. 铁账 */
class HammerEnchant056 : HammerEnchantBase(
    "铁账",
    "hammer_056",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 10.0, 15.0),
    "&7每 10 次命中额外获得 5 / 10 / 15 硬币，走 `PitGainCoinsEvent`",
    0.0,
    0
)
