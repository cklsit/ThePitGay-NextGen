package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.manager.PitManager
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/*
 * @Author ShanguanLinG
 * @Date 2025/9/12 13:13
 */

@ArmorOnly
class Regularity : AbstractEnchantment(), Listener, IAttackEntity, IPlayerDamaged {

    private val comboLayers = HashMap<UUID, Int>()
    private val pitAPI = ThePit.getApi()

    override fun getEnchantName(): String {
        return "狂暴连击"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "regularity"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RAGE_RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7当近战伤害低于&c${
            a(enchantLevel)
        }❤&7时, 将会自动再次攻击./s&7第二次攻击的伤害为第一次攻击的&c${
            b(enchantLevel)
        }%&7. &7(最多三次)/s" + "&7同时, 每攻击玩家 &e3 &7次时, 自身受到的伤害 &9-${c(enchantLevel)}% &7(可叠加, 最多三层)"
    }

    fun a(enchantLevel: Int): Double {
        return when (enchantLevel) {
            1 -> 0.7
            2 -> 1.7
            else -> 1.9
        }
    }

    fun b(enchantLevel: Int): Int {
        return when (enchantLevel) {
            1 -> 40
            2 -> 45
            else -> 60
        }
    }

    fun c(enchantLevel: Int): Int {
        return when (enchantLevel) {
            1 -> 2
            2 -> 3
            else -> 4
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun damage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager
        if (event.entity !is Player) return
        val player = (event.entity as Player).player
        if (attacker !is Player) return
        if (pitAPI.getItemEnchantLevel(player.inventory.leggings, "think_of_the_people") > 0) return

        val victim = event.entity
        if (victim !is Player) return

        if (PlayerUtil.shouldIgnoreEnchant(attacker, victim)) {
            return
        }

        val level = ThePit.api.getItemEnchantLevel(attacker.inventory.leggings, "regularity")
        if (level < 1) return

        if (event.finalDamage < a(level)) {
            val metadata = victim.getMetadata("regularity_cooldown")
            metadata.firstOrNull()?.asLong()?.let {
                if (System.currentTimeMillis() < it) {
                    return
                }
            }

            if (!victim.isDead) {
                val boost = b(level) * 0.01
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), {
                    victim.noDamageTicks = 0
                    victim.damage(event.damage * boost, attacker)
                    victim.setMetadata(
                        "regularity_cooldown",
                        FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 1000L + 2)
                    )
                }, 5L)
            }
        }
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (entity !is Player) return
        if (!entity.hasMetadata("regularity_cooldown")) return
        if (PitManager.hasAbsolutionHearts(entity)) return
        val cooldownEnd = entity.getMetadata("regularity_cooldown")[0].asLong()
        if (System.currentTimeMillis() < cooldownEnd - 600) { // 下一次理应正常触发的时间为500-505之间
            boostDamage.getAndSet(0.01)
        }
        val attackerUUID = player.uniqueId
        val layers = comboLayers.getOrDefault(attackerUUID, 0)
        if (layers < 3) comboLayers[attackerUUID] = comboLayers.getOrDefault(attackerUUID, 0) + 1
        if (layers <= 0) return
    }

    override fun handlePlayerDamaged(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        damage: Double,
        boostDamage: AtomicDouble,
        reduceDamage: AtomicDouble,
        cancel: AtomicBoolean
    ) {
        if (entity !is Player) return
        val attackerUUID = player.uniqueId
        if (pitAPI.getItemEnchantLevel(entity.inventory.leggings, "think_of_the_people") > 0) return
        val layers = comboLayers.getOrDefault(attackerUUID, 0)
        reduceDamage.getAndAdd(layers * c(enchantLevel) * -0.01)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent){
        comboLayers.remove(event.player.uniqueId)
    }
}