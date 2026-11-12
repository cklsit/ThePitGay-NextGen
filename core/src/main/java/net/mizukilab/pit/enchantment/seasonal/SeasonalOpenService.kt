package net.mizukilab.pit.enchantment.seasonal

import cn.charlotte.pit.ThePit
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import java.time.LocalDate
import java.time.ZoneId

/**
 * 季节性附魔开放判定。
 *
 * 判定基准：服务端真实日期（Asia/Shanghai），按自然月分季度，纯函数、不依赖重启。
 * 开放表（定稿 §8.1）：
 *   Q1(1-3)  → 91 / 92 / 93
 *   Q2(4-6)  → 94 / 95 / 96
 *   Q3(7-9)  → 97 / 98 / 99 / 100
 *   Q4(10-12)→ 全部 10 条（年度复刻窗口）
 *
 * 手动覆盖：主插件 config 的 `seasonal-override`（auto / force-open / force-close）。
 */
object SeasonalOpenService {

    private val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    private val QUARTER_IDS: Map<Int, Set<Int>> = mapOf(
        1 to setOf(91, 92, 93),
        2 to setOf(94, 95, 96),
        3 to setOf(97, 98, 99, 100),
        4 to setOf(91, 92, 93, 94, 95, 96, 97, 98, 99, 100)
    )

    /** 当前季度 1..4。 */
    @JvmStatic
    fun currentQuarter(): Int {
        val month = LocalDate.now(ZONE).monthValue
        return (month - 1) / 3 + 1
    }

    /** 本季度开放的季节性编号集合。 */
    @JvmStatic
    fun openIds(): Set<Int> = QUARTER_IDS[currentQuarter()] ?: emptySet()

    /** 是否开放。非季节性附魔恒为 true。 */
    @JvmStatic
    fun isOpen(enchantment: AbstractEnchantment): Boolean {
        if (enchantment.rarity != EnchantmentRarity.SEASONAL) {
            return true
        }
        return when (overrideMode()) {
            "force-open" -> true
            "force-close" -> false
            else -> {
                val id = seasonalId(enchantment) ?: return true
                id in openIds()
            }
        }
    }

    /** 从 nbtName（hammer_091 形式）解析季节性编号；非锤季节性返回 null。 */
    @JvmStatic
    fun seasonalId(enchantment: AbstractEnchantment): Int? {
        val name = enchantment.nbtName ?: return null
        if (!name.startsWith("hammer_")) {
            return null
        }
        return name.removePrefix("hammer_").toIntOrNull()
    }

    private fun overrideMode(): String = try {
        ThePit.getInstance().config.getString("seasonal-override", "auto") ?: "auto"
    } catch (e: Throwable) {
        "auto"
    }
}