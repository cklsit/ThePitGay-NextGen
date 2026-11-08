package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.utils.LocationUtil
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.Location
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@BowOnly
class LunarDeity : AbstractEnchantment(), Listener, IPlayerShootEntity,  IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "月神之矢"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "lunar_deity"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7射出箭矢时, 箭矢将锁定于自身中心的 &f" + checkRadius(enchantLevel) + " &7格内距离最近的目标 &7(" + (if (enchantLevel >= 4) 0 else 8L - (enchantLevel * 2L)) + "s冷却) /s" +
                "&7射出的箭矢速度将会加快, 同时命中目标时将获得 &3抗性提升 I &f(00:04)"
    }

    private fun checkRadius(enchantLevel: Int): Int {
        return (if (enchantLevel >= 3) enchantLevel * 8 else enchantLevel + 12)
    }

    @EventHandler
    fun onPlayerShootBow(e: EntityShootBowEvent) {
        if (e.projectile !is Arrow) return
        val arrow = e.projectile as Arrow

        if (arrow.shooter !is Player) return
        val shooter = arrow.shooter as Player
        val handItem = shooter.itemInHand

        val enchantLevel = ThePit.getApi().getItemEnchantLevel(handItem, this.nbtName)
        if (enchantLevel < 1 || PlayerUtil.shouldIgnoreEnchant(shooter)) return

        if (!Companion.cooldown.getOrDefault(shooter.uniqueId, Cooldown(0L))!!.hasExpired()) return

        val arrowLoc = arrow.location.clone()

        val nearestPlayer: Player? = LocationUtil.getNearestPlayer(arrow, checkRadius(enchantLevel).toDouble())

        if (nearestPlayer != null) {
            arrow.velocity = optimizedTrajectory(arrowLoc, nearestPlayer, arrow.velocity.length() + 0.2)
        }
    }

    private fun optimizedTrajectory(start: Location, target: Player, speed: Double): Vector {
        val targetHead = target.location.add(0.0, 1.55, 0.0)
        val tx = targetHead.x - start.x
        var ty = targetHead.y - start.y
        val tz = targetHead.z - start.z

        val hDistSq = tx * tx + tz * tz
        val hDist = sqrt(hDistSq)

        val gravityComp = 0.05
        ty += gravityComp * hDist

        val angle = atan2(ty, hDist)
        val yVel = sin(angle) * speed
        val hVel = cos(angle) * speed

        if (hDist > 1e-5) {
            return Vector(
                (tx / hDist) * hVel,
                yVel,
                (tz / hDist) * hVel
            )
        }
        return Vector(0.0, yVel, 0.0)
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        shooter: Player,
        target: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        atomicDouble1: AtomicDouble?,
        atomicBoolean: AtomicBoolean?
    ) {
        if (!Companion.cooldown.getOrDefault(shooter.uniqueId, Cooldown(0L))!!.hasExpired()) return
        Companion.cooldown.put(
            shooter.uniqueId,
            Cooldown(if (enchantLevel >= 4) 0 else 8L - (enchantLevel * 2L), TimeUnit.SECONDS)
        )
        if (shooter.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) shooter.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
        shooter.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 4, 0, false, true))
    }

    override fun getText(i: Int, player: Player): String? {
        if (PlayerUtil.isVenom(player)) return "&c&l✘"

        return getCooldownActionText(Companion.cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }

    companion object {
        private val cooldown = HashMap<UUID?, Cooldown?>()
    }
}
