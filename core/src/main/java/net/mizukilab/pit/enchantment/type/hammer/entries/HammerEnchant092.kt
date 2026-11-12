package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 92. 抽芽 */
class HammerEnchant092 : HammerEnchantBase(
    "抽芽",
    "hammer_092",
    EnchantmentRarity.SEASONAL,
    listOf(1.0, 2.0, 3.0),
    "&7每 10 次命中在落点生成短暂草地（粒子 + 逻辑区域），站在草地上时自身回复速度小幅提升",
    0.0,
    0,
    HammerEffect.SPECIAL
)