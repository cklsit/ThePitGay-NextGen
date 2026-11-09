package net.mizukilab.pit.enchantment.type.hammer

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤叠加封顶预算（C1 / C4）。
 *
 * - C1 自身减伤：同一次受击的合计减伤封顶 35%
 * - C4 单条命中增伤：同一次命中的合计增伤封顶 60%
 *
 * 各附魔处理器只向预算申请额度，由预算统一封顶，避免分散在十个文件里各自 clamp。
 * 额度窗口以最外层伤害事件的结算结束为界，每次攻击 / 受击都是独立窗口。
 */
object HammerBudget : Listener {

    private const val REDUCTION_CAP = 35.0
    private const val BONUS_CAP = 60.0

    private val usedReduction = ConcurrentHashMap<UUID, Double>()
    private val usedBonus = ConcurrentHashMap<UUID, Double>()

    /** 伤害事件的嵌套深度（伤害事件只在主线程派发）。 */
    private var depth = 0

    /** 申请增伤额度，返回本次实际获批的百分比。 */
    @JvmStatic
    fun claimBonus(entityId: UUID, requestedPct: Double): Double {
        return claim(usedBonus, entityId, requestedPct, BONUS_CAP)
    }

    /** 申请减伤额度，返回本次实际获批的百分比。 */
    @JvmStatic
    fun claimReduction(entityId: UUID, requestedPct: Double): Double {
        return claim(usedReduction, entityId, requestedPct, REDUCTION_CAP)
    }

    private fun claim(store: ConcurrentHashMap<UUID, Double>, entityId: UUID, requested: Double, cap: Double): Double {
        if (requested <= 0.0 || requested.isNaN()) return 0.0
        val current = store[entityId] ?: 0.0
        val room = (cap - current).coerceAtLeast(0.0)
        val granted = requested.coerceAtMost(room)
        if (granted > 0.0) store[entityId] = current + granted
        return granted
    }

    /**
     * 事件结算完成后清空额度。
     *
     * 附魔触发链内部会再次调用 `damage()`（溅射 / 反弹 / 分摊），
     * 形成嵌套事件；若在嵌套事件的 MONITOR 就清空，外层的累计值会被提前抹掉，
     * 因此用嵌套深度计数，只在最外层结束时统一清空。
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBegin(event: EntityDamageEvent) {
        depth++
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onResolve(event: EntityDamageEvent) {
        depth--
        if (depth <= 0) {
            depth = 0
            usedReduction.clear()
            usedBonus.clear()
        }
    }

    /** 玩家退出时清理，避免残留额度。 */
    fun clear(entityId: UUID) {
        usedReduction.remove(entityId)
        usedBonus.remove(entityId)
    }
}
