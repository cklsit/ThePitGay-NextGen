package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 18. 横抡 */
class HammerEnchant018 : HammerEnchantBase(
    "横抡",
    "hammer_018",
    EnchantmentRarity.NORMAL,
    listOf(4.0, 6.0, 8.0),
    "&7一次挥击命中 2 名及以上目标时按目标数增伤，上限 3 名",
    0.0,
    0
)
