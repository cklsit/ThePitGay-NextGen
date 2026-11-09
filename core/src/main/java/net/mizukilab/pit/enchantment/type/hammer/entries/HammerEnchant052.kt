package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 52. 铁砂掌 */
class HammerEnchant052 : HammerEnchantBase(
    "铁砂掌",
    "hammer_052",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中把目标向自身方向小幅拉回，冷却 8 秒",
    8.0,
    1
)
