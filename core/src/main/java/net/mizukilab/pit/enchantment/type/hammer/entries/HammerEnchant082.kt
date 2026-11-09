package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 82. 重砧契 */
class HammerEnchant082 : HammerEnchantBase(
    "重砧契",
    "hammer_082",
    EnchantmentRarity.RARE,
    listOf(2.0, 3.0, 4.0),
    "&7与主手重锤绑定，切换其他武器后 4 秒内减伤，但攻速降低",
    0.0,
    0
)
