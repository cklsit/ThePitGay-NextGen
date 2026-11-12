package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 16. 锤印 */
class HammerEnchant016 : HammerEnchantBase(
    "锤印",
    "hammer_016",
    EnchantmentRarity.NORMAL,
    listOf(3.0, 2.0, 1.0),
    "&7命中留下锤印，达到触发阈值时额外一次小额冲击并清空（上限 3 层、8 秒、切目标清空）",
    0.0,
    1,
    HammerEffect.SPECIAL
)