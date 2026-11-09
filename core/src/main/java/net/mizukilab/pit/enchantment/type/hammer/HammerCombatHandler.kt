package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.item.IMythicItem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

/**
 * 神话之锤附魔结算引擎（第一版）。
 *
 * 自动结算三类可数据化效果：
 * - DAMAGE_PCT：C4 组合计封顶 +60%
 * - DAMAGE_REDUCTION：C1 组合计封顶 35%
 * - HEAL：命中回复生命值（valueAt 单位 ❤，0.5 ❤ = 1 点生命值）
 *
 * SPECIAL 条目（状态/粒子/位移等）由后续专用处理器实现，本引擎跳过。
 */
class HammerCombatHandler : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val item = attacker.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return

        var damageBonus = 0.0
        var healHearts = 0.0
        for ((enchant, level) in mythic.enchantments) {
            if (enchant !is HammerEnchantBase) continue
            if (!SeasonalOpenService.isOpen(enchant)) continue
            when (enchant.effect()) {
                HammerEffect.DAMAGE_PCT -> damageBonus += enchant.valueAt(level)
                HammerEffect.HEAL -> healHearts += enchant.valueAt(level)
                else -> {
                }
            }
        }

        if (damageBonus > 0.0) {
            val capped = damageBonus.coerceAtMost(60.0)
            event.damage = event.damage * (1.0 + capped / 100.0)
        }
        if (healHearts > 0.0) {
            // 0.5 ❤ = 1 点生命值
            val newHealth = attacker.health + healHearts * 2.0
            attacker.health = newHealth.coerceAtMost(attacker.maxHealth)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        val item = victim.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return

        var reduction = 0.0
        for ((enchant, level) in mythic.enchantments) {
            if (enchant is HammerEnchantBase && SeasonalOpenService.isOpen(enchant)
                && enchant.effect() == HammerEffect.DAMAGE_REDUCTION) {
                reduction += enchant.valueAt(level)
            }
        }
        if (reduction > 0.0) {
            val capped = reduction.coerceAtMost(35.0)
            event.damage = event.damage * (1.0 - capped / 100.0)
        }
    }
}
