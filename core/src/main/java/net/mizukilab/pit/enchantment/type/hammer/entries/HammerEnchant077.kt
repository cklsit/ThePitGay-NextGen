package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 77. 铁潮 */
class HammerEnchant077 : HammerEnchantBase(
    "铁潮",
    "hammer_077",
    EnchantmentRarity.RARE,
    listOf(2.0, 3.0, 4.0),
    "&7命中 2 名以上目标后，接下来 4 秒内每次命中回复 0.5 ❤",
    0.0,
    0,
    HammerEffect.HEAL
)
