package cn.irina.thepitaddon.enchantment.type.limit

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.event.PitRegainHealthEvent
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.TimeUtil
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.ChatColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@WeaponOnly
class LonelyRedBlood : AbstractEnchantment(), IAttackEntity, IActionDisplayEnchant, Listener {
    private val cooldown: ConcurrentHashMap<UUID, Cooldown> = ConcurrentHashMap()
    private val pitAPI = ThePit.api
    private val healthCheckTaskMap: ConcurrentHashMap<UUID, BukkitRunnable> = ConcurrentHashMap()
    private val lastHealthMap: ConcurrentHashMap<UUID, Double> = ConcurrentHashMap()

    override fun getEnchantName(): String {
        return "孤红之恤: 吸血"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "lonely_red_blood"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7攻击恢复自身相当于伤害量 &c${enchantLevel * 4}% &7的生命 (上限&c1.5❤&7)/s" +
                "&7并对目标施加以下效果: /s" +
                "&7   &f▶ &4凝血 &f(${TimeUtil.formatTotalSeconds(if (enchantLevel >= 3) 4 else 2)})  /s" +
                "&7   &f▶ &8凋零 &f(${TimeUtil.formatTotalSeconds(if (enchantLevel >= 3) 4 else 2)}) /s" +
                "&7凝血与凋零效果每${28 - (enchantLevel * 4)}秒仅可触发一次 /s" +
                "&7效果 &4凝血&7: 无法通过&c任何途径&7恢复生命值 /s" +
                "&7效果 &8凋零&7: 持续缓慢地损失生命值"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        damage: Double,
        atomicDouble: AtomicDouble,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        val target = entity as? Player ?: return

        val healPercent = (enchantLevel * 4) / 100.0
        var healAmount = damage * healPercent

        if (healAmount > 1.5) {
            healAmount = 1.5
        }

        val newHealth = player.health + healAmount
        if (newHealth > player.maxHealth) {
            player.health = player.maxHealth
        } else {
            player.health = newHealth
        }

        stackBuff(target, if (enchantLevel >= 2) 4 else 2)

        if (target.hasPotionEffect(PotionEffectType.WITHER)) target.removePotionEffect(PotionEffectType.WITHER)
        target.addPotionEffect(
            PotionEffect(
                PotionEffectType.WITHER,
                (if (enchantLevel >= 2) 4 else 2) * 20,
                0,
                false,
                true
            )
        )
    }

    val buffCancelRunnableMap: ConcurrentHashMap<UUID, BukkitRunnable> = ConcurrentHashMap()
    private val activeBuffList: MutableList<UUID> = ArrayList()
    private fun stackBuff(player: Player, duration: Int) {
        activeBuffList.add(player.uniqueId)
        lastHealthMap[player.uniqueId] = player.health

        healthCheckTaskMap[player.uniqueId] = object : BukkitRunnable() {
            override fun run() {
                if (!activeBuffList.contains(player.uniqueId)) {
                    cancel()
                    healthCheckTaskMap.remove(player.uniqueId)
                    lastHealthMap.remove(player.uniqueId)
                    return
                }

                val currentHealth = player.health
                val lastHealth = lastHealthMap[player.uniqueId] ?: return

                if (currentHealth > lastHealth) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4&l凝血! &c你现在无法恢复生命值!"))
                    player.health = lastHealth
                } else {
                    lastHealthMap[player.uniqueId] = currentHealth
                }
            }
        }

        healthCheckTaskMap[player.uniqueId]!!.runTaskTimer(Main.instance, 0L, 1L)

        buffCancelRunnableMap[player.uniqueId] = object : BukkitRunnable() {
            override fun run() {
                activeBuffList.remove(player.uniqueId)
                buffCancelRunnableMap.remove(player.uniqueId)
                healthCheckTaskMap[player.uniqueId]?.cancel()
                healthCheckTaskMap.remove(player.uniqueId)
                lastHealthMap.remove(player.uniqueId)
                return
            }
        }

        buffCancelRunnableMap[player.uniqueId]!!.runTaskLaterAsynchronously(Main.instance, duration * 20L)
    }

    @EventHandler
    fun onHeal(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        if (!activeBuffList.contains(player.uniqueId)) return
        event.amount = 0.0
    }

    @EventHandler
    fun onHeal(event: PitRegainHealthEvent) {
        val player = event.player
        if (!activeBuffList.contains(player.uniqueId)) return
        event.amount = 0.0
    }

    override fun getText(p0: Int, p1: Player): String {
        return getCooldownActionText(cooldown.getOrDefault(p1.uniqueId, Cooldown(0L)))
    }
}
