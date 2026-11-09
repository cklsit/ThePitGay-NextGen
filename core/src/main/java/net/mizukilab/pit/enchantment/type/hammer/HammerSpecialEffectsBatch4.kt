package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤 SPECIAL 第四批：标记 / 区域 / 延迟伤害 / 临时属性。
 *
 * 覆盖：1 落砧 / 7 铁屑 / 10 铆钉 / 19 砂型 / 37 铆合 / 39 碎砂 / 40 石幔 /
 *       54 锻隙 / 55 钝鸣 / 58 冷砧 / 59 折返 / 62 锈痕 / 69 换砧 / 71 陨砧 /
 *       72 地脉 / 78 定盘 / 79 碎星 / 83 熔芯 / 84 断层 / 85 秤衡 / 87 回声砧 /
 *       89 沉星 / 91 惊蛰砧 / 94 骤雨 / 96 潮鸣 / 98 霜砧 / 99 落叶。
 */
class HammerSpecialEffectsBatch4 : Listener {

    // 1 落砧：命中计数与「已蓄力」标记
    private val hitCounter = ConcurrentHashMap<UUID, Int>()
    private val primed = ConcurrentHashMap.newKeySet<UUID>()

    // 通用标记：key = "<uuid>|<kind>" -> 到期时间
    private val marks = ConcurrentHashMap<String, Long>()

    // 59 折返：最近被击退时间
    private val knockedBack = ConcurrentHashMap<UUID, Long>()

    // 69 换砧：刚切换手持物品
    private val pendingSwitch = ConcurrentHashMap.newKeySet<UUID>()

    // 71 陨砧：最近重击时间
    private val lastHeavy = ConcurrentHashMap<UUID, Long>()

    // 87 回声砧：标记起点
    private val echoOrigin = ConcurrentHashMap<String, org.bukkit.Location>()

    // 89 沉星：霸体到期时间
    private val superArmor = ConcurrentHashMap<UUID, Long>()

    // 96 潮鸣：同一 tick 命中数
    private val burstTick = ConcurrentHashMap<UUID, Long>()
    private val burstCount = ConcurrentHashMap<UUID, Int>()

    // 区域：72 地脉 / 84 断层 / 92 抽芽 / 94 骤雨
    private val zones = java.util.concurrent.CopyOnWriteArrayList<HammerZone>()

    // 自身触发的二次伤害事件，避免递归
    private val guard = ConcurrentHashMap.newKeySet<UUID>()

    // ---------------------------------------------------------------- 攻击侧

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val target = event.entity as? LivingEntity ?: return
        if (guard.contains(target.uniqueId)) return
        val attacker = event.damager as? Player ?: return
        val mythic = hammerOf(attacker) ?: return

        // 19 砂型：被标记的目标暴击被压制
        if (marked(attacker.uniqueId, "sand") && isVanillaCrit(attacker)) {
            event.damage = event.damage * 0.5
        }

        // 55 钝鸣：被标记的输出者伤害降低
        val dull = markValue(attacker.uniqueId, "dull")
        if (dull > 0.0) {
            event.damage = event.damage * (1.0 - dull / 100.0)
        }

        // 10 铆钉：目标带标记时钝器伤害提高
        if (marked(target.uniqueId, "nail")) {
            event.damage = event.damage * 1.06
        }

        // 72 地脉：目标站在自己生成的裂隙内，受到的伤害提高
        if (inZone(attacker.uniqueId, "rift", target.location, 4.0)) {
            event.damage = event.damage * 1.08
        }

        // 91 惊蛰砧：目标处于标记状态时受到的击退提高
        if (marked(target.uniqueId, "shock")) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), Runnable {
                val dir = target.location.toVector().subtract(attacker.location.toVector()).setY(0)
                if (dir.lengthSquared() > 0.0001) {
                    target.velocity = target.velocity.add(dir.normalize().multiply(0.45))
                }
            })
        }

        // ---- 本命条件：以下仅当攻击者持有对应附魔时结算 ----

        // 1 落砧：每 4 次命中蓄力，下一次重击附带小范围冲击
        val lv1 = levelOf(mythic, "hammer_001")
        if (lv1 > 0) {
            val count = hitCounter.merge(attacker.uniqueId, 1, Int::plus) ?: 1
            if (count % 4 == 0) primed.add(attacker.uniqueId)
            if (primed.contains(attacker.uniqueId) && attacker.fallDistance > 1.5) {
                primed.remove(attacker.uniqueId)
                val pct = valueOf(mythic, "hammer_001")
                splashDamage(attacker, target, event.damage * (pct / 100.0), 2.0)
                spawn(attacker, target.location, Particle.SWEEP_ATTACK, 8, 0.8, 0.2, 0.8)
            }
        }

        // 92 抽芽：每 10 次命中在落点生成短暂草地
        if (levelOf(mythic, "hammer_092") > 0) {
            val count = hitCounter.merge(attacker.uniqueId, 1, Int::plus) ?: 1
            if (count % 10 == 0) {
                zones.add(HammerZone("grass", attacker.uniqueId, target.location.clone(), 3.0, System.currentTimeMillis() + 6000L))
                spawn(attacker, target.location, Particle.HAPPY_VILLAGER, 16, 1.0, 0.2, 1.0)
            }
        }

        // 7 铁屑：向 2 格内其他目标溅射小额伤害
        val lv7 = levelOf(mythic, "hammer_007")
        if (lv7 > 0) {
            val hearts = valueOf(mythic, "hammer_007")
            splashDamage(attacker, target, hearts * 2.0, 2.0)
        }

        // 10 铆钉：打标记
        val lv10 = levelOf(mythic, "hammer_010")
        if (lv10 > 0) {
            putMark(target.uniqueId, "nail", (valueOf(mythic, "hammer_010") * 1000).toLong())
        }

        // 19 砂型：压制目标的暴击
        val lv19 = levelOf(mythic, "hammer_019")
        if (lv19 > 0) {
            putMark(target.uniqueId, "sand", (valueOf(mythic, "hammer_019") * 1000).toLong())
        }

        // 37 铆合：短暂提升自身护甲
        val lv37 = levelOf(mythic, "hammer_037")
        if (lv37 > 0) {
            bufferAttribute(attacker, Attribute.ARMOR, "rivet", 4.0, (valueOf(mythic, "hammer_037") * 20).toLong().coerceAtLeast(20))
        }

        // 39 碎砂：禁止目标获得新增益
        val lv39 = levelOf(mythic, "hammer_039")
        if (lv39 > 0) {
            putMark(target.uniqueId, "sandlock", (valueOf(mythic, "hammer_039") * 1000).toLong())
        }

        // 54 锻隙：缩短自身受控时间
        val lv54 = levelOf(mythic, "hammer_054")
        if (lv54 > 0) {
            shortenControl(attacker, (valueOf(mythic, "hammer_054") * 20).toInt().coerceAtLeast(20))
        }

        // 55 钝鸣：对目标周围至多 N 名敌人施加减输出
        val lv55 = levelOf(mythic, "hammer_055")
        if (lv55 > 0) {
            val limit = lv55
            val duration = (valueOf(mythic, "hammer_055") * 1000).toLong().coerceAtLeast(2000L)
            val pct = if (lv55 >= 3) 16.0 else if (lv55 == 2) 12.0 else 8.0
            var applied = 0
            for (entity in target.getNearbyEntities(3.0, 3.0, 3.0)) {
                if (applied >= limit) break
                if (entity is Player && entity != attacker) {
                    putMarkValue(entity.uniqueId, "dull", pct, duration)
                    applied++
                }
            }
        }

        // 58 冷砧：延长目标已有减益
        val lv58 = levelOf(mythic, "hammer_058")
        if (lv58 > 0) {
            val extra = (valueOf(mythic, "hammer_058") * 20).toInt().coerceAtLeast(20)
            for (effect in target.activePotionEffects) {
                if (!isHarmful(effect.type)) continue
                target.addPotionEffect(
                    PotionEffect(effect.type, effect.duration + extra, effect.amplifier, effect.isAmbient, effect.hasParticles(), effect.hasIcon())
                )
            }
        }

        // 59 折返：被击退后的首次命中附带反推
        val lv59 = levelOf(mythic, "hammer_059")
        if (lv59 > 0) {
            val last = knockedBack[attacker.uniqueId] ?: 0L
            if (System.currentTimeMillis() - last <= 2000L) {
                knockedBack.remove(attacker.uniqueId)
                val dir = target.location.toVector().subtract(attacker.location.toVector()).setY(0)
                if (dir.lengthSquared() > 0.0001) {
                    target.velocity = dir.normalize().multiply(0.6).setY(0.25)
                }
            }
        }

        // 62 锈痕：目标装备增益短暂失效（护甲下调）
        val lv62 = levelOf(mythic, "hammer_062")
        if (lv62 > 0) {
            bufferAttribute(target, Attribute.ARMOR, "rust", -6.0, (valueOf(mythic, "hammer_062") * 20).toLong().coerceAtLeast(20))
        }

        // 69 换砧：切换手持物品后的首次命中
        if (levelOf(mythic, "hammer_069") > 0 && pendingSwitch.remove(attacker.uniqueId)) {
            event.damage = event.damage * 1.05
        }

        // 71 陨砧：蓄力重击的范围爆发 + 眩晕
        val lv71 = levelOf(mythic, "hammer_071")
        if (lv71 > 0 && attacker.fallDistance > 1.5) {
            val now = System.currentTimeMillis()
            val last = lastHeavy[attacker.uniqueId] ?: 0L
            if (now - last >= 12_000L) {
                lastHeavy[attacker.uniqueId] = now
                val pct = valueOf(mythic, "hammer_071")
                splashDamage(attacker, target, event.damage * (pct / 100.0), 3.0)
                target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, true))
                target.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false))
                spawn(attacker, target.location, Particle.EXPLOSION_EMITTER, 1, 0.0, 0.0, 0.0)
                attacker.playSound(attacker.location, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f)
            }
        }

        // 72 地脉：重击在地面留下裂隙
        if (levelOf(mythic, "hammer_072") > 0 && attacker.fallDistance > 1.5) {
            zones.add(HammerZone("rift", attacker.uniqueId, target.location.clone(), 4.0, System.currentTimeMillis() + 4000L))
            spawn(attacker, target.location, Particle.LAVA, 10, 1.2, 0.1, 1.2)
        }

        // 78 定盘：锁定目标物品栏切换
        val lv78 = levelOf(mythic, "hammer_078")
        if (lv78 > 0) {
            putMark(target.uniqueId, "lockitem", (valueOf(mythic, "hammer_078") * 1000).toLong())
            event.damage = event.damage + 0.5 * lv78
        }

        // 79 碎星：把目标护盾的一部分转为额外伤害
        val lv79 = levelOf(mythic, "hammer_079")
        if (lv79 > 0 && target.absorptionAmount > 0.01) {
            val key = attacker.uniqueId.toString() + "|shatter"
            val now = System.currentTimeMillis()
            if (now > (marks[key] ?: 0L)) {
                marks[key] = now + 8000L
                val converted = target.absorptionAmount * (valueOf(mythic, "hammer_079") / 100.0)
                target.absorptionAmount = (target.absorptionAmount - converted).coerceAtLeast(0.0)
                event.damage = event.damage + converted
                attacker.health = (attacker.health - 1.0).coerceAtLeast(1.0)
                spawn(attacker, target.location, Particle.CRIT, 12, 0.5, 0.6, 0.5)
            }
        }

        // 83 熔芯：立即结算一次持续伤害
        val lv83 = levelOf(mythic, "hammer_083")
        if (lv83 > 0) {
            for (type in listOf(PotionEffectType.POISON, PotionEffectType.WITHER)) {
                val effect = target.getPotionEffect(type) ?: continue
                val damage = 1.0 + effect.amplifier
                guard.add(target.uniqueId)
                try {
                    target.damage(damage, attacker)
                } finally {
                    guard.remove(target.uniqueId)
                }
            }
        }

        // 84 断层：在目标身后生成短暂阻挡
        if (levelOf(mythic, "hammer_084") > 0 && attacker.fallDistance > 1.5) {
            val back = target.location.clone().subtract(attacker.location.toVector().setY(0).normalize().multiply(1.2))
            zones.add(HammerZone("fault", attacker.uniqueId, back, 1.5, System.currentTimeMillis() + 1500L))
            spawn(attacker, back, Particle.CLOUD, 14, 0.6, 0.4, 0.6)
        }

        // 87 回声砧：命中后留下回声标记
        val lv87 = levelOf(mythic, "hammer_087")
        if (lv87 > 0) {
            putMark(target.uniqueId, "echo", (valueOf(mythic, "hammer_087") * 1000).toLong())
            echoOrigin[target.uniqueId.toString() + "|echo"] = target.location.clone()
        }

        // 89 沉星：落锤后进入短暂霸体
        if (levelOf(mythic, "hammer_089") > 0 && attacker.fallDistance > 1.5) {
            superArmor[attacker.uniqueId] = System.currentTimeMillis() + (valueOf(mythic, "hammer_089") * 1000).toLong()
        }

        // 91 惊蛰砧：打上「易被击退」标记
        val lv91 = levelOf(mythic, "hammer_091")
        if (lv91 > 0) {
            putMark(target.uniqueId, "shock", (valueOf(mythic, "hammer_091") * 1000).toLong())
        }

        // 94 骤雨：重击落点生成雨幕，站在其中命中时附带范围冲击
        if (levelOf(mythic, "hammer_094") > 0 && attacker.fallDistance > 1.5) {
            zones.add(HammerZone("rain", attacker.uniqueId, target.location.clone(), 3.0, System.currentTimeMillis() + 4000L))
            spawn(attacker, target.location, Particle.FALLING_WATER, 18, 1.2, 0.2, 1.2)
        }
        if (levelOf(mythic, "hammer_094") > 0 && inZone(attacker.uniqueId, "rain", attacker.location, 3.0)) {
            removeZone(attacker.uniqueId, "rain")
            splashDamage(attacker, target, 3.0, 3.0)
            spawn(attacker, target.location, Particle.SPLASH, 16, 1.2, 0.3, 1.2)
        }

        // 96 潮鸣：同时命中 2 名以上目标时提速
        val lv96 = levelOf(mythic, "hammer_096")
        if (lv96 > 0) {
            val nowTick = attacker.world.fullTime
            if (burstTick[attacker.uniqueId] != nowTick) {
                burstTick[attacker.uniqueId] = nowTick
                burstCount[attacker.uniqueId] = 1
            } else {
                val n = burstCount.merge(attacker.uniqueId, 1, Int::plus) ?: 1
                if (n >= 2) {
                    attacker.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 60, 0, false, true))
                }
            }
        }

        // 95 灼砧：生命值高于 60% 时命中短暂提升攻速
        val lv95 = levelOf(mythic, "hammer_095")
        if (lv95 > 0 && attacker.health >= attacker.maxHealth * 0.6) {
            bufferAttribute(
                attacker, Attribute.ATTACK_SPEED, "scorch",
                valueOf(mythic, "hammer_095") / 100.0, 60L, AttributeModifier.Operation.ADD_SCALAR
            )
            spawn(attacker, target.location, Particle.FLAME, 10, 0.4, 0.4, 0.4)
        }

        // 98 霜砧：留下霜痕，短暂降低目标攻速
        val lv98 = levelOf(mythic, "hammer_098")
        if (lv98 > 0) {
            bufferAttribute(target, Attribute.ATTACK_SPEED, "frost", -0.5, (valueOf(mythic, "hammer_098") * 20).toLong().coerceAtLeast(20))
            spawn(attacker, target.location, Particle.SNOWFLAKE, 12, 0.6, 0.1, 0.6)
        }

        // 99 落叶：命中处于减速状态的目标时追加范围伤害
        val lv99 = levelOf(mythic, "hammer_099")
        if (lv99 > 0 && target.getPotionEffect(PotionEffectType.SLOWNESS) != null) {
            val key = attacker.uniqueId.toString() + "|leaf"
            val now = System.currentTimeMillis()
            if (now > (marks[key] ?: 0L)) {
                marks[key] = now + 6000L
                splashDamage(attacker, target, valueOf(mythic, "hammer_099") * 2.0, 2.0)
                spawn(attacker, target.location, Particle.CHERRY_LEAVES, 14, 0.8, 0.6, 0.8)
            }
        }
    }

    // -------------------------------------------------------------- 受击侧

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        if (guard.contains(victim.uniqueId)) return
        val mythic = hammerOf(victim) ?: return

        // 59 折返：记录被击退
        if (event is EntityDamageByEntityEvent) {
            knockedBack[victim.uniqueId] = System.currentTimeMillis()
        }

        // 40 石幔：远程减伤（live ≥ 60% 生效）
        val lv40 = levelOf(mythic, "hammer_040")
        if (lv40 > 0 && liveRatio(mythic) >= 0.6 && event is EntityDamageByEntityEvent && event.damager is Projectile) {
            event.damage = event.damage * (1.0 - valueOf(mythic, "hammer_040") / 100.0)
        }

        // 85 秤衡：把本次伤害的一部分推迟到 4 秒内分摊
        val lv85 = levelOf(mythic, "hammer_085")
        if (lv85 > 0) {
            val pct = valueOf(mythic, "hammer_085") / 100.0
            val deferred = event.damage * pct
            if (deferred > 0.01) {
                event.damage = event.damage - deferred
                val uuid = victim.uniqueId
                for (i in 1..4) {
                    Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), Runnable {
                        if (!victim.isOnline) return@Runnable
                        guard.add(uuid)
                        try {
                            victim.damage(deferred / 4.0)
                        } finally {
                            guard.remove(uuid)
                        }
                    }, (i * 20).toLong())
                }
            }
        }

        // 89 沉星：霸体期间免疫击退
        val armorUntil = superArmor[victim.uniqueId] ?: 0L
        if (System.currentTimeMillis() < armorUntil) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), Runnable {
                victim.velocity = Vector(0.0, 0.0, 0.0)
            })
        }
    }

    // ---------------------------------------------------------- 其它监听器

    /** 87 回声砧：标记期间位移超过 3 格追加伤害。 */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!marked(player.uniqueId, "echo")) return
        val key = player.uniqueId.toString() + "|echo"
        val origin = echoOrigin[key] ?: return

        // 92 抽芽：站在自己的草地区域内持续小幅回复
        if (inZone(player.uniqueId, "grass", player.location, 3.0)) {
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, true))
        }

        // 84 断层：阻挡穿越
        if (blockedByFault(player)) {
            event.isCancelled = true
            return
        }

        if (origin.world == player.world && origin.distanceSquared(player.location) >= 9.0) {
            marks.remove(player.uniqueId.toString() + "|echo")
            echoOrigin.remove(key)
            guard.add(player.uniqueId)
            try {
                player.damage(3.0)
            } finally {
                guard.remove(player.uniqueId)
            }
            spawn(player, player.location, Particle.SONIC_BOOM, 1, 0.0, 0.0, 0.0)
        }
    }

    /** 78 定盘：锁定物品栏切换。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        if (marked(event.player.uniqueId, "lockitem")) {
            event.isCancelled = true
            return
        }
        pendingSwitch.add(event.player.uniqueId)
    }

    /** 39 碎砂：禁止目标获得新的增益效果。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (event.action != EntityPotionEffectEvent.Action.ADDED && event.action != EntityPotionEffectEvent.Action.CHANGED) return
        val effect = event.newEffect ?: return
        if (isHarmful(effect.type)) return
        if (marked(entity.uniqueId, "sandlock")) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onQuit(event: PlayerQuitEvent) {
        val id = event.player.uniqueId
        hitCounter.remove(id)
        primed.remove(id)
        knockedBack.remove(id)
        pendingSwitch.remove(id)
        lastHeavy.remove(id)
        superArmor.remove(id)
        burstTick.remove(id)
        burstCount.remove(id)
        guard.remove(id)
    }

    // ------------------------------------------------------------ 内部工具

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

    private fun liveRatio(mythic: IMythicItem): Double {
        if (mythic.maxLive <= 0) return 0.0
        return mythic.live.toDouble() / mythic.maxLive
    }

    /** 溅射伤害，避开攻击者并阻止递归。 */
    private fun splashDamage(attacker: Player, center: LivingEntity, amount: Double, radius: Double) {
        if (amount <= 0.0) return
        for (entity in center.getNearbyEntities(radius, radius, radius)) {
            if (entity !is LivingEntity) continue
            if (entity == attacker || entity == center) continue
            guard.add(entity.uniqueId)
            try {
                entity.damage(amount, attacker)
            } finally {
                guard.remove(entity.uniqueId)
            }
        }
    }

    private fun spawn(player: Player, loc: org.bukkit.Location, particle: Particle, count: Int, ox: Double, oy: Double, oz: Double) {
        player.world.spawnParticle(particle, loc, count, ox, oy, oz, 0.0)
    }

    private fun isVanillaCrit(player: Player): Boolean {
        return player.fallDistance > 0.0 && !player.isOnGround && !player.isInWater &&
                !player.isSprinting && !player.isInsideVehicle &&
                player.getPotionEffect(PotionEffectType.BLINDNESS) == null
    }

    private fun putMark(uuid: UUID, kind: String, millis: Long) {
        marks[uuid.toString() + "|" + kind] = System.currentTimeMillis() + millis
    }

    private fun marked(uuid: UUID, kind: String): Boolean {
        val until = marks[uuid.toString() + "|" + kind] ?: return false
        if (System.currentTimeMillis() > until) {
            marks.remove(uuid.toString() + "|" + kind)
            return false
        }
        return true
    }

    private fun putMarkValue(uuid: UUID, kind: String, value: Double, millis: Long) {
        markValues[uuid.toString() + "|" + kind] = value
        putMark(uuid, kind, millis)
    }

    private fun markValue(uuid: UUID, kind: String): Double {
        if (!marked(uuid, kind)) {
            markValues.remove(uuid.toString() + "|" + kind)
            return 0.0
        }
        return markValues[uuid.toString() + "|" + kind] ?: 0.0
    }

    /** 临时属性修饰符，到期自动移除。 */
    private fun bufferAttribute(
        entity: LivingEntity,
        attribute: Attribute,
        name: String,
        amount: Double,
        ticks: Long,
        operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER
    ) {
        val instance = entity.getAttribute(attribute) ?: return
        val key = NamespacedKey(ThePit.getInstance(), "hammer_temp_" + name)
        instance.modifiers.firstOrNull { it.key == key }?.let { instance.removeModifier(it) }
        instance.addModifier(
            AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY)
        )
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), Runnable {
            instance.modifiers.firstOrNull { it.key == key }?.let { instance.removeModifier(it) }
        }, ticks.coerceAtLeast(1L))
    }

    /** 缩短自身受控类效果的剩余时间。 */
    private fun shortenControl(player: Player, ticks: Int) {
        for (type in listOf(PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS)) {
            val effect = player.getPotionEffect(type) ?: continue
            val remain = effect.duration - ticks
            if (remain <= 0) {
                player.removePotionEffect(type)
            } else {
                player.addPotionEffect(
                    PotionEffect(type, remain, effect.amplifier, effect.isAmbient, effect.hasParticles(), effect.hasIcon())
                )
            }
        }
    }

    private fun inZone(owner: UUID, kind: String, loc: org.bukkit.Location, radius: Double): Boolean {
        val now = System.currentTimeMillis()
        var found = false
        for (zone in zones) {
            if (zone.expireAt <= now) {
                zones.remove(zone)
                continue
            }
            if (zone.owner != owner || zone.kind != kind) continue
            if (zone.center.world != loc.world) continue
            if (zone.center.distanceSquared(loc) <= radius * radius) found = true
        }
        return found
    }

    private fun removeZone(owner: UUID, kind: String) {
        zones.removeIf { it.owner == owner && it.kind == kind }
    }

    /** 84 断层：玩家穿过断层平面时阻挡。 */
    private fun blockedByFault(player: Player): Boolean {
        val now = System.currentTimeMillis()
        for (zone in zones) {
            if (zone.kind != "fault" || zone.expireAt <= now) continue
            if (zone.owner == player.uniqueId) continue
            if (zone.center.world != player.world) continue
            val dx = player.location.x - zone.center.x
            val dz = player.location.z - zone.center.z
            val dy = player.location.y - zone.center.y
            if (dy < -1.5 || dy > 2.5) continue
            if (dx * dx + dz * dz <= 1.6) return true
        }
        return false
    }

    private fun isHarmful(type: PotionEffectType): Boolean {
        return type == PotionEffectType.SLOWNESS || type == PotionEffectType.WEAKNESS ||
                type == PotionEffectType.POISON || type == PotionEffectType.WITHER ||
                type == PotionEffectType.BLINDNESS || type == PotionEffectType.NAUSEA ||
                type == PotionEffectType.MINING_FATIGUE || type == PotionEffectType.LEVITATION ||
                type == PotionEffectType.UNLUCK || type == PotionEffectType.DARKNESS ||
                type == PotionEffectType.HUNGER || type == PotionEffectType.INSTANT_DAMAGE ||
                type == PotionEffectType.SLOW_FALLING
    }

    private data class HammerZone(
        val kind: String,
        val owner: UUID,
        val center: org.bukkit.Location,
        val radius: Double,
        val expireAt: Long
    )

    private val markValues = ConcurrentHashMap<String, Double>()

    @Suppress("unused")
    private fun debug(player: Player, message: String) {
        player.sendMessage(CC.translate("&8[&6砧&8] &7$message"))
    }
}
