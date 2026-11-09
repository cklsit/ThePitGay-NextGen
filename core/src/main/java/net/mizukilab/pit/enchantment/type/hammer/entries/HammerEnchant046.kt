package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 46. 余烬 */
class HammerEnchant046 : HammerEnchantBase(
    "余烬",
    "hammer_046",
    EnchantmentRarity.NORMAL,
    listOf(3.0, 4.0, 5.0),
    "&7生命值首次跌破 30% 时短暂提升移速，冷却 30 秒",
    30.0,
    0,
    HammerEffect.SPECIAL
)
