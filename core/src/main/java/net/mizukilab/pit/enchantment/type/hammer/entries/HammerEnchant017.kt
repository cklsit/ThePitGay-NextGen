package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 17. 提锤 */
class HammerEnchant017 : HammerEnchantBase(
    "提锤",
    "hammer_017",
    EnchantmentRarity.NORMAL,
    listOf(3.0, 9.0, 12.0),
    "&7收锤时间越长伤害越高，2 秒封顶",
    0.0,
    0,
    HammerEffect.SPECIAL
)