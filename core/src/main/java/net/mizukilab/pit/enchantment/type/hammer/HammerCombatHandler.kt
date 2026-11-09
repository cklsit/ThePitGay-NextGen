package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤附魔结算引擎。
 *
 * 自动结算：
 * - DAMAGE_PCT：C4 组合计封顶 +60%
 * - DAMAGE_REDUCTION：C1 组合计封顶 35%
 * - HEAL：命中回复生命值（valueAt 单位 ❤，0.5 ❤ = 1 点生命值）
 * - live 消耗：按条目自身冷却触发，单 tick 合计封顶 2 点
 * - 猛击（下落攻击）：额外伤害封顶 +50%
 *
 * SPECIAL 条目（状态/粒子/位移）由后续专用处理器补。
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
        var liveCost = 0

        for ((enchant, level) in mythic.enchantments) {
            if (enchant !is HammerEnchantBase) continue
            if (!SeasonalOpenService.isOpen(enchant)) continue
            when (enchant.effect()) {
                HammerEffect.DAMAGE_PCT -> damageBonus += enchant.valueAt(level)
                HammerEffect.HEAL -> healHearts += enchant.valueAt(level)
                else -> {
                }
            }
            val cost = enchant.liveCostPerTrigger()
            if (cost > 0 && tryTrigger(attacker.uniqueId, enchant)) {
                liveCost += cost
            }
        }

        if (damageBonus > 0.0) {
            val capped = damageBonus.coerceAtMost(60.0)
            event.damage = event.damage * (1.0 + capped / 100.0)
        }

        // 猛击（下落 > 1.5 格）：额外伤害封顶 +50%
        if (attacker.fallDistance > 1.5) {
            val base = (attacker.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE)?.value ?: 1.0).coerceAtLeast(1.0)
            val cap = base * 1.5
            if (event.damage > cap) {
                event.damage = cap
            }
        }

        if (healHearts > 0.0) {
            val newHealth = attacker.health + healHearts * 2.0
            attacker.health = newHealth.coerceAtMost(attacker.maxHealth)
        }

        if (liveCost > 0) {
            consumeLive(attacker, mythic, liveCost.coerceAtMost(2))
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

    private fun consumeLive(player: Player, mythic: IMythicItem, cost: Int) {
        val current = mythic.live
        if (current <= 0) return
        val next = (current - cost).coerceAtLeast(0)
        if (next == current) return
        mythic.live = next
        player.inventory.setItemInMainHand(mythic.toItemStack())
    }

    /** 按条目冷却判定是否可触发；可触发则记录时间并返回 true。 */
    private fun tryTrigger(playerId: UUID, enchant: HammerEnchantBase): Boolean {
        val cooldown = enchant.cooldownMillis()
        if (cooldown <= 0) {
            return true
        }
        val key = playerId.toString() + "|" + enchant.nbtName
        val now = System.currentTimeMillis()
        val last = LAST_TRIGGER[key] ?: 0L
        if (now - last < cooldown) {
            return false
        }
        LAST_TRIGGER[key] = now
        return true
    }

    companion object {
        private val LAST_TRIGGER = ConcurrentHashMap<String, Long>()
    }
}
