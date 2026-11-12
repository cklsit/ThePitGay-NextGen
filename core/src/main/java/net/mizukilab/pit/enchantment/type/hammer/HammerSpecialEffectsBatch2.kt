package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤 SPECIAL 第二批。
 *
 * 覆盖：20 熔渣 / 75 引锤 / 67 铁腥 / 16 锤印 / 21 淬口 / 33 碾轮 /
 *       57 落点尺 / 68 落砂 / 74 崩砧 / 88 压铸 / 90 绝契 / 97 收锭。
 */
class HammerSpecialEffectsBatch2 : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val item = attacker.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return
        val target = event.entity as? LivingEntity ?: return
        val world = target.world
        val loc = target.location

        fun levelOf(nbt: String): Int = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
        }?.value ?: 0

        // 20 熔渣：落点减速区域
        levelOf("hammer_020").takeIf { it > 0 }?.let { lv ->
            val radius = 2.5
            val secs = 2 + lv
            world.getNearbyEntities(loc, radius, radius, radius).forEach { e ->
                if (e is LivingEntity && e != attacker) {
                    e.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, secs * 20, 0, false, true))
                }
            }
            world.spawnParticle(Particle.LAVA, loc, 12, 1.0, 0.2, 1.0, 0.0)
        }

        // 75 引锤：向落点聚拢
        levelOf("hammer_075").takeIf { it > 0 }?.let {
            world.getNearbyEntities(loc, 3.5, 3.5, 3.5).forEach { e ->
                if (e is LivingEntity && e != attacker) {
                    val dir = loc.toVector().subtract(e.location.toVector()).setY(0)
                    if (dir.lengthSquared() > 0.0001) {
                        e.velocity = dir.normalize().multiply(0.45).setY(0.2)
                    }
                }
            }
        }

        // 57 落点尺：粒子圆环
        if (levelOf("hammer_057") > 0) {
            val radius = 2.0 + levelOf("hammer_057")
            for (i in 0 until 24) {
                val angle = Math.PI * 2 * i / 24
                val x = loc.x + Math.cos(angle) * radius
                val z = loc.z + Math.sin(angle) * radius
                world.spawnParticle(Particle.CRIT, x, loc.y + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }

        // 68 落砂：DARKNESS 视野遮蔽
        levelOf("hammer_068").takeIf { it > 0 && target is Player }?.let { lv ->
            (target as Player).addPotionEffect(PotionEffect(PotionEffectType.DARKNESS, (lv + 1) * 20, 0, false, true))
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 20, 1.0, 0.3, 1.0, 0.01)
        }

        // 16 锤印：叠层到阈值后追加一次冲击
        val markLevel = levelOf("hammer_016")
        if (markLevel > 0) {
            val key = attacker.uniqueId.toString() + "|" + target.uniqueId
            val stack = MARK_STACK.merge(key, 1, Int::plus) ?: 1
            if (stack >= markLevel) {
                MARK_STACK[key] = 0
                target.damage(2.0, attacker)
                world.spawnParticle(Particle.CRIT, loc, 12, 0.5, 0.5, 0.5, 0.2)
                attacker.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.4f)
            }
        }

        // 21 淬口：每 5 击追加无视护甲伤害
        levelOf("hammer_021").takeIf { it > 0 }?.let { lv ->
            val hits = HIT_COUNTER.merge(attacker.uniqueId, 1, Int::plus) ?: 1
            if (hits % 5 == 0) {
                val extra = (0.5 + (lv - 1) * 0.25) * 2.0
                target.health = (target.health - extra).coerceAtLeast(0.0)
            }
        }

        // 67 铁腥：给目标打上禁疗标记
        levelOf("hammer_067").takeIf { it > 0 && target is Player }?.let { lv ->
            ANTI_HEAL[(target as Player).uniqueId.toString() + "|antibeal"] =
                System.currentTimeMillis() + lv * 1000L
        }

        // 33 碾轮：同一目标连续 3 次命中追加范围伤害
        levelOf("hammer_033").takeIf { it > 0 }?.let { lv ->
            val key = attacker.uniqueId.toString() + "|t|" + target.uniqueId
            val c = MARK_STACK.merge(key, 1, Int::plus) ?: 1
            if (c % 3 == 0) {
                val dmg = (0.5 + (lv - 1) * 0.25) * 2.0
                world.getNearbyEntities(loc, 2.0, 2.0, 2.0).forEach { e ->
                    if (e is LivingEntity && e != attacker) {
                        e.damage(dmg, attacker)
                    }
                }
                world.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.6, 0.3, 0.6, 0.0)
            }
        }

        // 74 崩砧：live = 1 时爆发（C1 优先级低于 53 砧垫，保命条目在场时不触发）
        if (mythic.live == 1 && levelOf("hammer_074") > 0 && levelOf("hammer_053") <= 0) {
            val bonus = levelOf("hammer_074").let { if (it >= 3) 30.0 else if (it == 2) 24.0 else 18.0 }
            val granted = HammerBudget.claimBonus(attacker.uniqueId, bonus)
            if (granted > 0.0) {
                event.damage = event.damage * (1.0 + granted / 100.0)
                // 触发后 live 归零（按 §5.2 该物品不可用），未获批额度则不扣
                mythic.live = 0
                attacker.inventory.setItemInMainHand(mythic.toItemStack())
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }

        // 88 压铸：目标 3 种以上减益时增伤
        levelOf("hammer_088").takeIf { it > 0 }?.let { lv ->
            val debuffs = target.activePotionEffects.count {
                it.type == PotionEffectType.SLOWNESS || it.type == PotionEffectType.WEAKNESS ||
                        it.type == PotionEffectType.POISON || it.type == PotionEffectType.WITHER ||
                        it.type == PotionEffectType.MINING_FATIGUE || it.type == PotionEffectType.DARKNESS
            }
            if (debuffs >= 3) {
                val bonus = if (lv >= 3) 20.0 else if (lv == 2) 15.0 else 10.0
                val granted = HammerBudget.claimBonus(attacker.uniqueId, bonus)
                if (granted > 0.0) event.damage = event.damage * (1.0 + granted / 100.0)
            }
        }

        // 90 绝契：目标 3 层以上负面时清除自身负面
        if (levelOf("hammer_090") > 0) {
            val debuffs = target.activePotionEffects.count { isNegative(it.type) }
            if (debuffs >= 3) {
                attacker.activePotionEffects.filter { isNegative(it.type) }.forEach {
                    attacker.removePotionEffect(it.type)
                }
                attacker.playSound(attacker.location, Sound.ITEM_TOTEM_USE, 0.6f, 1.2f)
            }
        }
    }

    /** 67 铁腥：目标无法使用治疗类物品（在受击后 1-3 秒内拦截）。 */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val key = player.uniqueId.toString() + "|antibeal"
        val until = ANTI_HEAL[key] ?: 0L
        if (System.currentTimeMillis() < until) {
            event.isCancelled = true
            player.sendMessage(CC.translate("&c你被「铁腥」压制, 无法使用治疗物品!"))
        }
    }

    /** 97 收锭：击杀按目标连杀获得额外经验，且本次击杀不消耗 live。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onKill(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val item = killer.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == "hammer_097" && SeasonalOpenService.isOpen(it.key)
        } ?: return
        val lv = entry.value
        val victimProfile = PlayerProfile.getRawCache(event.entity.uniqueId)
        val streak = victimProfile?.streakKills ?: 0.0
        val exp = (streak * 0.5 * lv).coerceAtMost(500.0)
        val killerProfile = PlayerProfile.getRawCache(killer.uniqueId)
        if (killerProfile != null && exp > 0.0) {
            killerProfile.experience = killerProfile.experience + exp
            killer.sendMessage(CC.translate("&f&l收锭! &7获得 &b${exp.toInt()} &7经验"))
        }
    }

    private fun isNegative(type: PotionEffectType): Boolean {
        return type == PotionEffectType.SLOWNESS || type == PotionEffectType.WEAKNESS ||
                type == PotionEffectType.POISON || type == PotionEffectType.WITHER ||
                type == PotionEffectType.BLINDNESS || type == PotionEffectType.DARKNESS ||
                type == PotionEffectType.MINING_FATIGUE || type == PotionEffectType.NAUSEA
    }

    companion object {
        private val MARK_STACK = ConcurrentHashMap<String, Int>()
        private val HIT_COUNTER = ConcurrentHashMap<UUID, Int>()
        private val ANTI_HEAL = ConcurrentHashMap<String, Long>()
    }
}