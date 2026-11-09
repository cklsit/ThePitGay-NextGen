package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤 SPECIAL 第五批：挥空判定 / 击杀续航 / 叠层减伤 / 队友辅助。
 *
 * 覆盖：4 回摆 / 9 余温 / 28 覆砸 / 31 收势 / 34 退火 / 41 铁砧回声 /
 *       47 铁水 / 60 双层锭 / 73 千锻 / 77 铁潮 / 82 重砧契。
 */
class HammerSpecialEffectsBatch5 : Listener {

    // 4 回摆 / 31 收势：挥空计数
    private val swings = ConcurrentHashMap<UUID, Int>()

    // 41 铁砧回声：目标 -> "到期时间|回复❤|施放者"
    private val echoMark = ConcurrentHashMap<UUID, String>()

    // 60 双层锭：层数
    private val plateStacks = ConcurrentHashMap<UUID, Int>()

    // 73 千锻：连续命中同一目标
    private val forgeHits = ConcurrentHashMap<String, Int>()

    // 77 铁潮：群战期间回血到期时间
    private val tideUntil = ConcurrentHashMap<UUID, Long>()

    // 34 退火 / 82 重砧契：减伤到期时间
    private val annealUntil = ConcurrentHashMap<UUID, Long>()
    private val annealActive = ConcurrentHashMap<UUID, Long>()
    private val contractUntil = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.NORMAL)
    fun onSwing(event: PlayerAnimationEvent) {
        if (event.animationType != PlayerAnimationType.ARM_SWING) return
        if (!ItemTypes.isMythicHammer(event.player.inventory.itemInMainHand)) return
        swings.merge(event.player.uniqueId, 1, Int::plus)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val target = event.entity as? LivingEntity ?: return
        val mythic = hammerOf(attacker) ?: return

        val swing = swings[attacker.uniqueId] ?: 0
        val missed = swing >= 2
        swings[attacker.uniqueId] = 0

        // 4 回摆：挥空后的下一次命中增伤
        if (missed && levelOf(mythic, "hammer_004") > 0) {
            val granted = HammerBudget.claimBonus(attacker.uniqueId, valueOf(mythic, "hammer_004"))
            if (granted > 0.0) event.damage = event.damage * (1.0 + granted / 100.0)
        }

        // 31 收势：连续 3 次挥空后命中回血
        if (missed && swing >= 4 && levelOf(mythic, "hammer_031") > 0) {
            heal(attacker, valueOf(mythic, "hammer_031"))
            attacker.sendMessage(CC.translate("&6&l收势! &7挥空后的命中回复了生命值."))
        }

        // 28 覆砸：目标处于击退或腾空状态时增伤
        if (levelOf(mythic, "hammer_028") > 0) {
            val airborne = !target.isOnGround || target.velocity.lengthSquared() > 0.09
            if (airborne) {
                val granted = HammerBudget.claimBonus(attacker.uniqueId, valueOf(mythic, "hammer_028"))
                if (granted > 0.0) event.damage = event.damage * (1.0 + granted / 100.0)
            }
        }

        // 41 铁砧回声：命中打标记，队友命中该目标时回复生命值
        val lv41 = levelOf(mythic, "hammer_041")
        if (lv41 > 0) {
            echoMark[target.uniqueId] = (System.currentTimeMillis() + (valueOf(mythic, "hammer_041") * 1000).toLong()).toString() +
                    "|" + valueOf(mythic, "hammer_041") + "|" + attacker.uniqueId
        } else {
            // 由任意锤子持有者施加的回声标记，任意玩家（含施加者以外）命中时回血
            val mark = echoMark[target.uniqueId]
            if (mark != null) {
                val parts = mark.split("|")
                val expiry = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                if (System.currentTimeMillis() > expiry) {
                    echoMark.remove(target.uniqueId)
                } else {
                    val owner = parts.getOrNull(2)
                    if (owner != null && owner != attacker.uniqueId.toString()) {
                        heal(attacker, 0.5)
                    }
                }
            }
        }

        // 47 铁水：命中处于减速区域的目标时回复生命值
        val lv47 = levelOf(mythic, "hammer_047")
        if (lv47 > 0 && target.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
            heal(attacker, valueOf(mythic, "hammer_047"))
        }

        // 60 双层锭：连续命中叠层，受击清空一半
        val lv60 = levelOf(mythic, "hammer_060")
        if (lv60 > 0) {
            val cap = lv60 + 1
            plateStacks.merge(attacker.uniqueId, 1, Int::plus)
            if ((plateStacks[attacker.uniqueId] ?: 0) > cap) plateStacks[attacker.uniqueId] = cap
        }

        // 73 千锻：连续命中同一目标回复生命并小幅提速
        val lv73 = levelOf(mythic, "hammer_073")
        if (lv73 > 0) {
            val key = attacker.uniqueId.toString() + "|" + target.uniqueId
            val hits = forgeHits.merge(key, 1, Int::plus) ?: 1
            if (hits >= 2) {
                heal(attacker, 0.25)
                buffSkill(attacker, "forge", 0.05, 40L)
            }
            if (hits == 1) {
                forgeHits.keys.removeIf { it.startsWith(attacker.uniqueId.toString() + "|") && it != key }
            }
        }

        // 77 铁潮：命中 2 名以上目标后进入群战续航
        if (levelOf(mythic, "hammer_077") > 0) {
            val near = target.getNearbyEntities(2.5, 2.5, 2.5).count { it is Player && it != attacker }
            if (near >= 2) {
                tideUntil[attacker.uniqueId] = System.currentTimeMillis() + 4000L
            }
            val until = tideUntil[attacker.uniqueId] ?: 0L
            if (System.currentTimeMillis() < until) {
                heal(attacker, 0.5)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return

        // 82 重砧契：主手已换成其他武器时仍需生效，故先于「主手是否为锤」判定
        if (System.currentTimeMillis() < (contractUntil[victim.uniqueId] ?: 0L)) {
            val granted = HammerBudget.claimReduction(victim.uniqueId, 8.0)
            if (granted > 0.0) {
                event.damage = event.damage * (1.0 - granted / 100.0)
            }
        }

        val mythic = hammerOf(victim) ?: return

        // 60 双层锭：按层数减伤，受击清空一半层数
        val lv60 = levelOf(mythic, "hammer_060")
        if (lv60 > 0) {
            val stacks = plateStacks[victim.uniqueId] ?: 0
            if (stacks > 0) {
                val granted = HammerBudget.claimReduction(victim.uniqueId, 2.0 * stacks)
                if (granted > 0.0) {
                    event.damage = event.damage * (1.0 - granted / 100.0)
                }
                plateStacks[victim.uniqueId] = stacks / 2
            }
        }

        // 34 退火：受到暴击后进入一段缓冲期，期间降低后续受到的伤害
        val lv34 = levelOf(mythic, "hammer_034")
        if (lv34 > 0 && event is EntityDamageByEntityEvent) {
            val damager = event.damager as? Player
            if (damager != null && isVanillaCrit(damager)) {
                val now = System.currentTimeMillis()
                if (now > (annealUntil[victim.uniqueId] ?: 0L)) {
                    annealUntil[victim.uniqueId] = now + 10_000L
                    annealActive[victim.uniqueId] = now + (valueOf(mythic, "hammer_034") * 1000).toLong()
                }
            }
        }
        if (System.currentTimeMillis() < (annealActive[victim.uniqueId] ?: 0L)) {
            val granted = HammerBudget.claimReduction(victim.uniqueId, 8.0)
            if (granted > 0.0) {
                event.damage = event.damage * (1.0 - granted / 100.0)
            }
        }

        // 82 重砧契：切换其他武器后的缓冲减伤（命中判定已在方法入口处理）
    }

    /** 9 余温：击杀后 3 秒内持续回复生命值。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onKill(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val mythic = hammerOf(killer) ?: return
        val lv9 = levelOf(mythic, "hammer_009")
        if (lv9 <= 0) return
        val hearts = valueOf(mythic, "hammer_009")
        repeat(3) { i ->
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), Runnable {
                if (killer.isOnline) heal(killer, hearts / 3.0)
            }, (20L * (i + 1)))
        }
        killer.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, killer.location.add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.01)
    }

    /** 82 重砧契：切换非锤武器后进入缓冲减伤（同时攻速降低）。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val next = player.inventory.getItem(event.newSlot) ?: return
        if (ItemTypes.isMythicHammer(next)) return
        val hammer = findHammer(player) ?: return
        val lv82 = levelOf(hammer, "hammer_082")
        if (lv82 <= 0) return
        contractUntil[player.uniqueId] = System.currentTimeMillis() + 4000L
        return
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onQuit(event: PlayerQuitEvent) {
        val id = event.player.uniqueId
        swings.remove(id)
        plateStacks.remove(id)
        tideUntil.remove(id)
        annealUntil.remove(id)
        annealActive.remove(id)
        contractUntil.remove(id)
        forgeHits.keys.removeIf { it.startsWith(id.toString() + "|") }
    }

    // ------------------------------------------------------------ 内部工具

    private fun findHammer(player: Player): IMythicItem? {
        for (stack in player.inventory.contents) {
            if (stack == null || !ItemTypes.isMythicHammer(stack)) continue
            val mythic = ThePit.getInstance().itemFactory.getItemFromStack(stack) as? IMythicItem ?: continue
            if (!mythic.enchantments.isEmpty()) return mythic
        }
        return null
    }

    private fun hammerOf(player: Player): IMythicItem? {
        val item = player.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return null
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return null
        return if (mythic.enchantments.isEmpty()) null else mythic
    }

    private fun levelOf(mythic: IMythicItem, nbt: String): Int {
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
        } ?: return 0
        return entry.value
    }

    private fun valueOf(mythic: IMythicItem, nbt: String): Double {
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
        } ?: return 0.0
        return (entry.key as HammerEnchantBase).valueAt(entry.value)
    }

    private fun heal(player: Player, hearts: Double) {
        if (hearts <= 0.0) return
        player.health = (player.health + hearts * 2.0).coerceAtMost(player.maxHealth)
    }

    private fun isVanillaCrit(player: Player): Boolean {
        return player.fallDistance > 0.0 && !player.isOnGround && !player.isInWater &&
                !player.isSprinting && !player.isInsideVehicle &&
                player.getPotionEffect(PotionEffectType.BLINDNESS) == null
    }

    private fun buffSkill(player: Player, name: String, scalar: Double, ticks: Long) {
        val instance = player.getAttribute(Attribute.ATTACK_SPEED) ?: return
        val key = org.bukkit.NamespacedKey(ThePit.getInstance(), "hammer_temp_" + name)
        instance.modifiers.firstOrNull { it.key == key }?.let { instance.removeModifier(it) }
        instance.addModifier(AttributeModifier(key, scalar, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY))
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), Runnable {
            instance.modifiers.firstOrNull { it.key == key }?.let { instance.removeModifier(it) }
        }, ticks.coerceAtLeast(1L))
    }
}
