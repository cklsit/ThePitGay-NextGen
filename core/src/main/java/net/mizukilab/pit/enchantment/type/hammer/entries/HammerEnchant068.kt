package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 68. 落砂 */
class HammerEnchant068 : HammerEnchantBase(
    "落砂",
    "hammer_068",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中在目标脚下扬起砂尘，以粒子 + `DARKNESS` 短暂遮蔽远距离视野，冷却 9 秒",
    9.0,
    1
)
