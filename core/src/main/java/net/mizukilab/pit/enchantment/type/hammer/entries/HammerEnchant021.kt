package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 21. 淬口 */
class HammerEnchant021 : HammerEnchantBase(
    "淬口",
    "hammer_021",
    EnchantmentRarity.NORMAL,
    listOf(5.0, 0.5, 0.75),
    "&7每 5 次命中附带一次无视护甲的小额伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)