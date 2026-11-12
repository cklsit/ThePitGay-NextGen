package net.mizukilab.pit.enchantment.type.hammer

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.HammerOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.cooldown.Cooldown
import java.util.concurrent.TimeUnit

/**
 * 神话之锤附魔公共基类。
 * 每条附魔只声明名称 / nbt / 稀有度 / tier 1-2-3 数值 / lore 模板 / 冷却 / live 消耗，
 * 具体结算由 HammerCombatHandler 统一处理（C1-C4 封顶、M 组互斥、季节短路）。
 */
@HammerOnly
abstract class HammerEnchantBase(
    private val enchantName: String,
    private val nbtName: String,
    private val rarity: EnchantmentRarity,
    private val tierValues: List<Double>,
    private val loreTemplate: String,
    private val cooldownSeconds: Double = 0.0,
    private val liveCost: Int = 0,
    private val effect: HammerEffect = HammerEffect.SPECIAL
) : AbstractEnchantment() {

    override fun getEnchantName(): String = enchantName

    override fun getMaxEnchantLevel(): Int = 3

    override fun getNbtName(): String = nbtName

    override fun getRarity(): EnchantmentRarity = rarity

    override fun getCooldown(): Cooldown? =
        if (cooldownSeconds > 0) Cooldown((cooldownSeconds * 1000).toLong(), TimeUnit.MILLISECONDS) else null

    override fun getUsefulnessLore(level: Int): String =
        loreTemplate.replace("{v}", formatValue(valueAt(level)))

    /** tier 1 / 2 / 3 对应值；越界取最后一档。 */
    fun valueAt(level: Int): Double =
        tierValues.getOrElse((level - 1).coerceAtLeast(0)) { tierValues.lastOrNull() ?: 0.0 }

    fun liveCostPerTrigger(): Int = liveCost

    fun effect(): HammerEffect = effect

    fun cooldownMillis(): Long = (cooldownSeconds * 1000).toLong()

    private fun formatValue(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
}