package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 64. 铸缝 */
class HammerEnchant064 : HammerEnchantBase(
    "铸缝",
    "hammer_064",
    EnchantmentRarity.NORMAL,
    listOf(1.0, 2.0, 3.0),
    "&7每次进入神话之井附魔时额外保留 live 不被消耗",
    0.0,
    0
)
