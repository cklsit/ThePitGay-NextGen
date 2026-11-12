package net.mizukilab.pit.enchantment.type.hammer.entries

import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.enchantment.type.hammer.HammerEffect
import net.mizukilab.pit.enchantment.type.hammer.HammerEnchantBase

/** 86. 铁誓 */
class HammerEnchant086 : HammerEnchantBase(
    "铁誓",
    "hammer_086",
    EnchantmentRarity.RARE,
    listOf(1.0, 2.0, 3.0),
    "&7每次击杀叠加一层铁誓，每层 +3% 伤害，死亡清空",
    0.0,
    0,
    HammerEffect.SPECIAL
)