package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 3. 砧纹 */
class HammerEnchant003 : HammerEnchantBase(
    "砧纹",
    "hammer_003",
    EnchantmentRarity.NORMAL,
    listOf(2.0, 3.0, 4.0),
    "&7连续命中同一目标叠层增伤，切换目标或 3 秒未命中即清空",
    0.0,
    0,
    HammerEffect.DAMAGE_PCT
)