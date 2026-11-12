package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 35. 范线 */
class HammerEnchant035 : HammerEnchantBase(
    "范线",
    "hammer_035",
    EnchantmentRarity.NORMAL,
    listOf(0.3, 0.5, 0.7),
    "&7略微扩大有效命中范围（服务端补偿判定，不改客户端 3 格）",
    0.0,
    0,
    HammerEffect.SPECIAL
)