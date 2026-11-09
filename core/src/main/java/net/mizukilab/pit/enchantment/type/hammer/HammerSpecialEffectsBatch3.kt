package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤 SPECIAL 第三批：护盾 / 保命 / 解控 / 低血加速 / 以伤换续航。
 *
 * 覆盖：44 锭壳 / 53 砧垫 / 93 融雪 / 46 余烬 / 45 欠火。
 */
class HammerSpecialEffectsBatch3 : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val item = attacker.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return

        fun value(nbt: String): Double {
            val e = mythic.enchantments.entries.firstOrNull {
                it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
            } ?: return 0.0
            return (e.key as HammerEnchantBase).valueAt(e.value)
        }

        // 45 欠火：自身伤害降低，但每次命中回复生命值
        val v45 = value("hammer_045")
        if (v45 > 0.0) {
            event.damage = event.damage * (1.0 - v45 / 100.0)
            // 回复值按 tier 递增：0.25 / 0.5 / 0.75 ❤
            val lvEntry = mythic.enchantments.entries.firstOrNull {
                it.key is HammerEnchantBase && it.key.nbtName == "hammer_045"
            }
            val lv = lvEntry?.value ?: 1
            val hearts = 0.25 * lv
            attacker.health = (attacker.health + hearts * 2.0).coerceAtMost(attacker.maxHealth)
        }

        // 93 融雪：命中清除自身减速并短暂提速
        val v93 = value("hammer_093")
        if (v93 > 0.0) {
            attacker.removePotionEffect(PotionEffectType.SLOWNESS)
            attacker.addPotionEffect(PotionEffect(PotionEffectType.SPEED, (v93 * 20).toInt().coerceAtLeast(20), 1, false, true))
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return

        // 46 余烬：生命值首次跌破 30% 时短暂提速
        if (victim.health - event.finalDamage <= victim.maxHealth * 0.3) {
            val item = victim.inventory.itemInMainHand
            if (ItemTypes.isMythicHammer(item)) {
                val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem
                val entry = mythic?.enchantments?.entries?.firstOrNull {
                    it.key is HammerEnchantBase && it.key.nbtName == "hammer_046" && SeasonalOpenService.isOpen(it.key)
                }
                if (entry != null) {
                    val key = victim.uniqueId.toString() + "|ember"
                    val until = COOLDOWN[key] ?: 0L
                    if (System.currentTimeMillis() > until) {
                        COOLDOWN[key] = System.currentTimeMillis() + 30_000L
                        val secs = (3 + entry.value - 1) * 20
                        victim.addPotionEffect(PotionEffect(PotionEffectType.SPEED, secs.coerceAtLeast(20), 1, false, true))
                        victim.sendMessage(CC.translate("&6&l余烬! &7生命告急, 获得短暂移速!"))
                    }
                }
            }
        }

        val item = victim.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return

        fun value(nbt: String): Double {
            val e = mythic.enchantments.entries.firstOrNull {
                it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
            } ?: return 0.0
            return (e.key as HammerEnchantBase).valueAt(e.value)
        }

        // 44 锭壳：护盾层（吸收），耗尽后 12 秒重建
        val v44 = value("hammer_044")
        if (v44 > 0.0) {
            val key = victim.uniqueId.toString() + "|shield"
            val until = COOLDOWN[key] ?: 0L
            if (victim.absorptionAmount <= 0.01 && System.currentTimeMillis() > until) {
                victim.absorptionAmount = v44
                COOLDOWN[key] = System.currentTimeMillis() + 12_000L
            }
        }

        // 53 砧垫：致命伤保留 1 点生命，消耗等量 live
        val v53 = value("hammer_053")
        if (v53 > 0.0 && victim.health - event.finalDamage <= 0.0) {
            val key = victim.uniqueId.toString() + "|cushion"
            val until = COOLDOWN[key] ?: 0L
            if (System.currentTimeMillis() > until && mythic.live > 0) {
                event.isCancelled = true
                victim.health = 1.0
                mythic.live = (mythic.live - 1).coerceAtLeast(0)
                victim.inventory.setItemInMainHand(mythic.toItemStack())
                COOLDOWN[key] = System.currentTimeMillis() + 60_000L
                victim.sendMessage(CC.translate("&c&l砧垫! &7你以 1 点物品生命换回了一条命!"))
            }
        }
    }

    companion object {
        private val COOLDOWN = ConcurrentHashMap<String, Long>()
    }
}
