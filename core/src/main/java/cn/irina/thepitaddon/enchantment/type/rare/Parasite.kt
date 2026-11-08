package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.TimeUtil.formatTotalSeconds
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@BowOnly
class Parasite : AbstractEnchantment(),  Listener, IPlayerShootEntity, IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "寄生"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "parasite"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        val duration: Int = BASE_DURATION + (enchantLevel * DURATION_PER_LEVEL)
        val lore = StringBuilder()
            .append("&7当满蓄力的箭矢命中目标时，为目标施加 &2寄生 &f(")
            .append(formatTotalSeconds(duration))
            .append(") ")

        if (enchantLevel < 4) {
            val cooldown = 22 - (enchantLevel * 4)
            lore.append("&7(").append(cooldown).append("s冷却)")
        }

        return lore.append("/s&7并对目标劈下基础伤害为 &c${if (enchantLevel >= 2) 4 else 2}❤ &7的闪电并附带 &8缓慢 IV &f(00:02) &7效果")
            .append("/s&7若持有此效果的目标攻击其他任意目标时，&2寄生 &7效果将被提前解除/s")
            .append("&7效果 &2寄生&7: 每秒受到 &c0.5❤ 必中&7伤害")
            .toString()
    }

    @EventHandler
    fun onShoot(event: EntityShootBowEvent) {
        if (event.getEntity() !is Player) return
        val shooter = event.getEntity() as Player

        if (ThePit.getApi().getItemEnchantLevel(shooter.itemInHand, this.nbtName) < 1) return
        if (event.force >= FULL_CHARGE) {
            ARROW_FORCES.put(shooter.uniqueId, event.force)
        }
    }

    @EventHandler
    fun onDamage(event: PlayerDeathEvent) {
        val attacker = event.entity.killer
        if (attacker == null) return

        val task: BukkitTask? = EFFECT_TASKS[attacker.uniqueId]
        if (task == null) return
        attacker.sendMessage(CC.translate("&2&l寄生! &7效果已解除!"))
        task.cancel()
        EFFECT_TASKS.remove(attacker.uniqueId)
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        shooter: Player,
        entity: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        atomicDouble1: AtomicDouble?,
        atomicBoolean: AtomicBoolean?
    ) {
        if (entity !is Player) return
        val target = entity
        val shooterId = shooter.uniqueId

        if (ARROW_FORCES.getOrDefault(shooterId, 0f)!! < FULL_CHARGE || !cooldowns.getOrDefault(shooterId, Cooldown(0L))!!.hasExpired()) return
        PlayerUtil.playThunderEffect(target.location)
        PlayerUtil.damage(target, PlayerUtil.DamageType.NORMAL, (if (enchantLevel >= 2) 8 else 4).toDouble(), true)

        if (target.hasPotionEffect(PotionEffectType.SLOW)) target.removePotionEffect(PotionEffectType.SLOW)
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 2 * 20, 3, false, true))

        target.sendMessage(CC.translate("&2&l寄生! &7你被施加了 &2寄生 &7效果!"))

        val cooldown = 22 - (enchantLevel * 4)
        cooldowns.put(shooter.uniqueId, Cooldown(cooldown.toLong(), TimeUnit.SECONDS))

        // 清理旧任务
        cancelExistingTask(target.uniqueId)

        EFFECT_TASKS.put(target.uniqueId, object : BukkitRunnable() {
            val MaxCD: Int = BASE_DURATION + (enchantLevel * DURATION_PER_LEVEL)
            var cd: Int = 1

            override fun run() {
                if (!validateTarget(target) || cd >= MaxCD) {
                    target.sendMessage(CC.translate("&2&l寄生! &7效果已解除!"))
                    cancelTask(target.uniqueId)
                    return
                }

                cd += 1
                applyTrueDamage(target)
                shooter.sendMessage(CC.translate("&2&l寄生! &f" + target.displayName + " &7当前剩余血量: &c" + (target.health / 2).toInt() + "❤"))
            }
        }.runTaskTimer(Main.instance, 0L, DAMAGE_INTERVAL.toLong()))
    }

    // 辅助方法
    private fun cancelExistingTask(targetId: UUID?) {
        val existingTask: BukkitTask? = EFFECT_TASKS[targetId]
        existingTask?.cancel()
    }

    private fun cancelTask(targetId: UUID?) {
        cancelExistingTask(targetId)
        EFFECT_TASKS.remove(targetId)
    }

    private fun validateTarget(target: Player): Boolean {
        val tp = PlayerProfile.getRawCache(target.uniqueId)
        return target.isOnline && tp != null && tp.isInArena
    }

    private fun applyTrueDamage(target: Player) {
        PlayerUtil.damage(target, PlayerUtil.DamageType.TRUE, TRUE_DAMAGE, false)
    }

    override fun getText(i: Int, player: Player): String? {
        if (!PlayerUtil.isVenom(player)) return getCooldownActionText(
            cooldowns.getOrDefault(
                player.uniqueId,
                Cooldown(0L)
            )
        )

        return "&c&l✘"
    }

    companion object {
        // 常量定义
        private const val FULL_CHARGE = 1.0f
        private const val BASE_DURATION = 2
        private const val DURATION_PER_LEVEL = 2
        private const val DAMAGE_INTERVAL = 20 // 单位：tick (20 ticks = 1秒)
        private const val TRUE_DAMAGE = 1.0

        // 使用线程安全的ConcurrentHashMap
        private val EFFECT_TASKS: MutableMap<UUID?, BukkitTask?> = ConcurrentHashMap<UUID?, BukkitTask?>()
        private val ARROW_FORCES: MutableMap<UUID?, Float?> = ConcurrentHashMap<UUID?, Float?>()
        private val cooldowns: MutableMap<UUID?, Cooldown?> = ConcurrentHashMap<UUID?, Cooldown?>()
    }
}