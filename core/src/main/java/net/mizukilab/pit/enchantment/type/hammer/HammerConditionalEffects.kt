package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤「条件增伤 / 条件减伤」批量实现（第三批）。
 *
 * 增伤：2 配重 / 3 砧纹 / 6 惯性 / 14 钝锋 / 15 秤砣 / 17 提锤 / 18 横抡 /
 *       24 锭心 / 25 断纹 / 32 坠点 / 36 击桩 / 38 承力 / 42 毛边 / 48 砧裂 /
 *       61 沉锤 / 66 断锤 / 70 老锤 / 81 逆锤 / 86 铁誓
 * 减伤：5 沉肩 / 8 冷锻 / 22 砧座 / 26 铁律
 */
class HammerConditionalEffects : Listener {

    // uuid -> 最后攻击时间
    private val lastAttack = ConcurrentHashMap<UUID, Long>()
    // uuid -> 最后移动时间
    private val lastMove = ConcurrentHashMap<UUID, Long>()
    // uuid -> 最后受伤时间
    private val lastDamaged = ConcurrentHashMap<UUID, Long>()
    // uuid -> 连续命中同一目标计数
    private val sameTargetHits = ConcurrentHashMap<String, Int>()
    // uuid -> 铁誓层数
    private val oathStacks = ConcurrentHashMap<UUID, Int>()

    @EventHandler(priority = EventPriority.NORMAL)
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            lastMove[event.player.uniqueId] = System.currentTimeMillis()
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onQuit(event: PlayerQuitEvent) {
        val id = event.player.uniqueId
        lastAttack.remove(id)
        lastMove.remove(id)
        lastDamaged.remove(id)
        oathStacks.remove(id)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onKill(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        if (!ItemTypes.isMythicHammer(killer.inventory.itemInMainHand)) return
        oathStacks.merge(killer.uniqueId, 1, Int::plus)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val item = attacker.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return
        val target = event.entity as? LivingEntity ?: return

        val now = System.currentTimeMillis()
        val sinceLastAttack = now - (lastAttack[attacker.uniqueId] ?: 0L)
        lastAttack[attacker.uniqueId] = now

        fun value(nbt: String): Double {
            val e = mythic.enchantments.entries.firstOrNull {
                it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
            } ?: return 0.0
            return (e.key as HammerEnchantBase).valueAt(e.value)
        }

        var bonus = 0.0

        // 2 配重：收锤 > 1.2s 首次命中
        if (sinceLastAttack > 1200) bonus += value("hammer_002")

        // 3 砧纹：连续命中同一目标叠层（上限 3 层）
        val v3 = value("hammer_003")
        if (v3 > 0.0) {
            val key = attacker.uniqueId.toString() + "|" + target.uniqueId
            val stack = (sameTargetHits.merge(key, 1, Int::plus) ?: 1).coerceAtMost(3)
            bonus += v3 * stack
        }

        // 6 惯性：移动中命中
        if (attacker.isSprinting || attacker.velocity.lengthSquared() > 0.01) bonus += value("hammer_006")

        // 14 钝锋：目标有护甲
        if (target is Player && target.inventory.armorContents.any { it != null && it.type != org.bukkit.Material.AIR }) {
            bonus += value("hammer_014")
        }

        // 15 秤砣：目标血量比例高于自身
        if (target.health / target.maxHealth > attacker.health / attacker.maxHealth) bonus += value("hammer_015")

        // 17 提锤：收锤越久越高，2s 封顶，每秒 +3%
        if (sinceLastAttack > 0) {
            val seconds = (sinceLastAttack / 1000.0).coerceAtMost(2.0)
            val v17 = value("hammer_017")
            if (v17 > 0.0) bonus += (seconds * 3.0).coerceAtMost(v17)
        }

        // 18 横抡：命中 2 名以上
        val v18 = value("hammer_018")
        if (v18 > 0.0) {
            val nearby = target.world.getNearbyEntities(target.location, 3.0, 3.0, 3.0)
                .count { it is LivingEntity && it != attacker && it != target }
            if (nearby >= 1) bonus += v18 * nearby.coerceAtMost(2)
        }

        // 24 锭心：live 满
        if (mythic.live >= mythic.maxLive) bonus += value("hammer_024")

        // 25 断纹：目标血量低于一半
        if (target.health < target.maxHealth * 0.5) bonus += value("hammer_025")

        // 32 坠点：从高处落下
        if (attacker.fallDistance > 0.5) bonus += value("hammer_032")

        // 36 击桩：目标 1s 内未移动
        if (target is Player) {
            val last = lastMove[target.uniqueId] ?: 0L
            if (now - last > 1000) bonus += value("hammer_036")
        }

        // 38 承力：受伤后 2s 内
        if (now - (lastDamaged[attacker.uniqueId] ?: 0L) < 2000) bonus += value("hammer_038")

        // 42 毛边：目标减益种类
        val v42 = value("hammer_042")
        if (v42 > 0.0) {
            val debuffs = target.activePotionEffects.count { isNegative(it.type) }
            if (debuffs > 0) bonus += (v42 + (debuffs - 1)).coerceAtMost(v42 + 3.0)
        }

        // 48 砧裂：live 低于一半
        if (mythic.live < mythic.maxLive * 0.5) bonus += value("hammer_048")

        // 61 沉锤：目标生命上限高于自身
        if (target is Player && target.maxHealth > attacker.maxHealth) bonus += value("hammer_061")

        // 66 断锤：目标无法移动
        if (target.hasPotionEffect(PotionEffectType.SLOWNESS) || target.hasPotionEffect(PotionEffectType.NAUSEA)) {
            bonus += value("hammer_066")
        }

        // 70 老锤：live 越低越高，每 10% 一档
        val v70 = value("hammer_070")
        if (v70 > 0.0 && mythic.maxLive > 0) {
            val missing = (1.0 - mythic.live.toDouble() / mythic.maxLive).coerceIn(0.0, 1.0)
            // 每缺失 10% 叠一档，最多 10 档
            bonus += v70 * (missing * 10).coerceAtMost(10.0)
        }

        // 81 逆锤：自身血量低于 30%
        if (attacker.health < attacker.maxHealth * 0.3) bonus += value("hammer_081")

        // 86 铁誓：击杀叠层
        val stacks = oathStacks[attacker.uniqueId] ?: 0
        if (stacks > 0 && value("hammer_086") > 0.0) bonus += stacks * 3.0

        if (bonus > 0.0) {
            val granted = HammerBudget.claimBonus(attacker.uniqueId, bonus)
            if (granted > 0.0) {
                event.damage = event.damage * (1.0 + granted / 100.0)
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onDamaged(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        lastDamaged[victim.uniqueId] = System.currentTimeMillis()
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

        val now = System.currentTimeMillis()
        val standingFor = now - (lastMove[victim.uniqueId] ?: 0L)
        var reduction = 0.0

        // 5 沉肩：站定 0.8s
        if (standingFor > 800) reduction += value("hammer_005")
        // 8 冷锻：常驻
        reduction += value("hammer_008")
        // 22 砧座：站定 1s
        if (standingFor > 1000) reduction += value("hammer_022")
        // 26 铁律：自身被减速/控制
        if (victim.hasPotionEffect(PotionEffectType.SLOWNESS) || victim.hasPotionEffect(PotionEffectType.NAUSEA)) {
            reduction += value("hammer_026")
        }

        if (reduction > 0.0) {
            val granted = HammerBudget.claimReduction(victim.uniqueId, reduction)
            if (granted > 0.0) {
                event.damage = event.damage * (1.0 - granted / 100.0)
            }
        }
    }

    private fun isNegative(type: PotionEffectType): Boolean {
        return type == PotionEffectType.SLOWNESS || type == PotionEffectType.WEAKNESS ||
                type == PotionEffectType.POISON || type == PotionEffectType.WITHER ||
                type == PotionEffectType.BLINDNESS || type == PotionEffectType.DARKNESS ||
                type == PotionEffectType.MINING_FATIGUE || type == PotionEffectType.NAUSEA
    }
}
