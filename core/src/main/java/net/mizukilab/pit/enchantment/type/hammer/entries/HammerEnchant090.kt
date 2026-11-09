package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 90. 绝契 */
class HammerEnchant090 : HammerEnchantBase(
    "绝契",
    "hammer_090",
    EnchantmentRarity.RARE,
    listOf(3.0, 3.0, 3.0),
    "&7命中携带 3 层以上负面状态的目标时，清除自身全部负面状态并造成一次小额冲击，冷却 20 秒",
    20.0,
    0
)
