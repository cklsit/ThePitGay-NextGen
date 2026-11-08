package cn.irina.thepitaddon.enchantment.type.limit

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.data.PhantasmState
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Bukkit
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/*
 * @Author Irina
 * @Date 2025/10/3 23:59
 */

@ArmorOnly
class Phantasm : AbstractEnchantment(), Listener, IActionDisplayEnchant {

    override fun getEnchantName(): String = "虚影"
    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.OP
    override fun getCooldown(): Cooldown? = null
    override fun getMaxEnchantLevel(): Int = 3
    override fun getNbtName(): String = "phantasm"

    override fun getUsefulnessLore(level: Int): String {
        val durationSeconds = level * DURATION_PER_LEVEL_SECONDS
        val speedBonus = (level * SPEED_MULTIPLIER_PER_LEVEL * 100).toInt()
        return "&7穿戴附有此附魔的 &e神话之甲 &7时 /s" +
                "&7单击下蹲键将触发效果 &8虚化 (${durationSeconds}s) &7(${COOLDOWN_SECONDS}s冷却) /s" +
                "&7效果 &8虚化&7: &7在 &e${durationSeconds} &7秒内无法被攻击, 在此期间移速增加 &b${speedBonus}% /s" +
                "&7但同时, 自身也无法攻击目标"
    }

    private val instance = Main.instance
    private val pitApi = ThePit.getApi()
    private val stateMap = ConcurrentHashMap<UUID, PhantasmState>()
    private val lastAccessTimeMap = ConcurrentHashMap<UUID, Long>()

    companion object {
        // 核心配置
        private const val COOLDOWN_SECONDS = 9L
        private const val DURATION_PER_LEVEL_SECONDS = 0.75
        private const val BASE_DURATION_TICKS = 15L
        private const val SPEED_MULTIPLIER_PER_LEVEL = 0.15F

        // 完美虚化配置
        private const val PERFECT_DODGE_WINDOW_MS = 600L
        private const val PERFECT_DURATION_MULTIPLIER = 1.2
        private const val PERFECT_HEAL = 8.0

        // 清理配置
        private const val CLEANUP_INTERVAL_TICKS = 20L * 60L * 20L // 20分钟
        private const val STATE_EXPIRE_TIME_MS = 30L * 60L * 1000L // 30分钟

        // 消息常量
        private const val MSG_ACTIVATE = "&8かむい!"
        private const val MSG_PERFECT = "&8虚影 &7完美虚化!"
        private const val MSG_CANNOT_ATTACK = "&8虚影 &7虚化状态下无法攻击!"
    }

    init {
        startCleanupTask()
    }

    // ==================== 状态管理 ====================

    private fun getState(player: Player): PhantasmState {
        lastAccessTimeMap[player.uniqueId] = System.currentTimeMillis()
        return stateMap.getOrPut(player.uniqueId) { PhantasmState() }
    }

    private fun Player.scheduleDeactivation(state: PhantasmState, durationTicks: Long) {
        state.task?.cancel()
        state.task = Bukkit.getScheduler().runTaskLater(instance, {
            deactivatePhantasm(state)
        }, durationTicks)
    }

    private fun Player.deactivatePhantasm(state: PhantasmState) {
        state.isActive = false
        state.isPerfect = false
        if (isOnline) {
            walkSpeed = state.defaultSpeed
        }
        state.task = null
    }

    // ==================== 事件处理 ====================

    @EventHandler
    fun onSneak(evt: PlayerToggleSneakEvent) {
        if (!evt.player.isSneaking) return
        evt.player.tryActivatePhantasm()
    }

    private fun Player.tryActivatePhantasm() {
        val level = inventory.leggings?.let { pitApi.getItemEnchantLevel(it, nbtName) } ?: 0
        if (level < 1) return

        val state = getState(this)
        if (!state.cooldown.hasExpired()) return

        // 激活虚影状态
        state.apply {
            cooldown = Cooldown(COOLDOWN_SECONDS, TimeUnit.SECONDS)
            this.level = level
            startTime = System.currentTimeMillis()
            isActive = true
        }

        sendMessage(CC.translate(MSG_ACTIVATE))
        walkSpeed = state.defaultSpeed * (1 + level * SPEED_MULTIPLIER_PER_LEVEL)
        scheduleDeactivation(state, calculateDuration(level))
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onDefense(evt: EntityDamageByEntityEvent) {
        val victim = evt.entity as? Player ?: return
        val state = getState(victim)
        if (!state.isActive) return

        evt.isCancelled = true

        // 检查是否触发完美虚化
        if (state.shouldTriggerPerfectDodge()) {
            victim.handlePerfectDodge(state)
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onAttack(evt: EntityDamageByEntityEvent) {
        val attacker = evt.extractAttacker() ?: return
        val state = getState(attacker)
        if (!state.isActive) return

        evt.isCancelled = true
        attacker.sendMessage(CC.translate(MSG_CANNOT_ATTACK))
    }

    private fun PhantasmState.shouldTriggerPerfectDodge(): Boolean {
        if (isPerfect) return false
        val timeSinceActivation = System.currentTimeMillis() - startTime
        return timeSinceActivation <= PERFECT_DODGE_WINDOW_MS
    }

    private fun Player.handlePerfectDodge(state: PhantasmState) {
        sendMessage(CC.translate(MSG_PERFECT))
        state.isPerfect = true
        PlayerUtil.heal(this, PERFECT_HEAL)
        scheduleDeactivation(state, calculateDuration(state.level, isPerfect = true))
    }

    private fun EntityDamageByEntityEvent.extractAttacker(): Player? = when (val dmg = damager) {
        is Arrow -> dmg.shooter as? Player
        is Player -> dmg
        else -> null
    }

    @EventHandler
    fun onQuit(evt: PlayerQuitEvent) = cleanupPlayer(evt.player)

    @EventHandler
    fun onKick(evt: PlayerKickEvent) = cleanupPlayer(evt.player)

    @EventHandler
    fun onDeath(evt: org.bukkit.event.entity.PlayerDeathEvent) = cleanupPlayer(evt.entity)

    // ==================== 工具方法 ====================

    private fun cleanupPlayer(player: Player) {
        stateMap.remove(player.uniqueId)?.also { state ->
            lastAccessTimeMap.remove(player.uniqueId)
            state.cleanUp(player)
        }
    }

    private fun calculateDuration(level: Int, isPerfect: Boolean = false): Long {
        val baseDuration = level * BASE_DURATION_TICKS
        return if (isPerfect) {
            (baseDuration * PERFECT_DURATION_MULTIPLIER).toLong()
        } else {
            baseDuration
        }
    }

    private fun startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, {
            performPeriodicCleanup()
        }, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS)
    }

    private fun performPeriodicCleanup() {
        val currentTime = System.currentTimeMillis()
        val iterator = lastAccessTimeMap.entries.iterator()
        var cleanupCount = 0

        // 使用 iterator 一次遍历完成删除，避免 ConcurrentModificationException
        while (iterator.hasNext()) {
            val (uuid, lastTime) = iterator.next()
            if (currentTime - lastTime > STATE_EXPIRE_TIME_MS) {
                iterator.remove()
                stateMap.remove(uuid)?.cleanUp()
                cleanupCount++
            }
        }

        if (cleanupCount > 0) {
            Bukkit.getLogger().info("[Phantasm] 清理了 $cleanupCount 个过期的玩家状态")
        }
    }

    override fun getText(p0: Int, p: Player): String {
        return getCooldownActionText(getState(p).cooldown)
    }
}