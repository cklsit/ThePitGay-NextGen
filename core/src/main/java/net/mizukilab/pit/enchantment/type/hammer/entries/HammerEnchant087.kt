package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 87. 回声砧 */
class HammerEnchant087 : HammerEnchantBase(
    "回声砧",
    "hammer_087",
    EnchantmentRarity.RARE,
    listOf(2.0, 3.0, 4.0),
    "&7命中后 2 秒内，若目标移动超过 3 格则追加一次小额伤害并清空标记",
    0.0,
    0,
    HammerEffect.SPECIAL
)