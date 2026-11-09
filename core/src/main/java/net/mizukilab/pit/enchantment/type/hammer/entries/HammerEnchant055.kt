package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 55. 钝鸣 */
class HammerEnchant055 : HammerEnchantBase(
    "钝鸣",
    "hammer_055",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7命中时对目标周围至多 N 名敌人施加 2 / 3 / 4 秒「对他人造成的伤害 -8% / -12% / -16%」，不造成伤害",
    0.0,
    0,
    HammerEffect.SPECIAL
)
