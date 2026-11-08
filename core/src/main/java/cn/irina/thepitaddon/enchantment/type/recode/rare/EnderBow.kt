package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.LocationUtil
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

/*
 * @Author Irina
 * @Date 2025/6/11 16:48
 */

@BowOnly
class EnderBow: AbstractEnchantment(), IPlayerShootEntity, IActionDisplayEnchant, Listener {
    override fun getEnchantName(): String {
        return "末影弓"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "ender_bow_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(level: Int): String {
        return StringBuilder("&7射击时若自身处于潜行状态, 射出的箭矢会将自身传送至箭矢落地点 (${if (level >= 3) 25 else 120 - (level * 30)}s冷却) /s")
            .append("&7(传送后视角将转向以自身为中心 &e10 &7格内距离自身最近的目标) /s")
            .append("&7(箭矢命中目标时减少冷却3s)")
            .toString()
    }

    private val cooldown = ConcurrentHashMap<UUID, Cooldown>()
    override fun handleShootEntity(
        level: Int,
        player: Player,
        entity: Entity,
        p3: Double,
        p4: AtomicDouble?,
        p5: AtomicDouble?,
        p6: AtomicBoolean?,
    ) {
        if (cooldown[player.uniqueId] == null || cooldown[player.uniqueId]!!.hasExpired()) return
        cooldown[player.uniqueId] = Cooldown(max(0L, cooldown[player.uniqueId]!!.remaining - 3000L))
    }

    @EventHandler
    fun onShoot(event: EntityShootBowEvent) {
        if (ThePit.getInstance().eventFactory.activeEpicEvent != null) return

        val player = event.entity as? Player ?: return
        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return

        val itemInHand = player.itemInHand
        if (itemInHand == null || itemInHand.type == Material.AIR || itemInHand.type != Material.BOW) return

        val level = ThePit.getApi().getItemEnchantLevel(itemInHand, this.nbtName)
        if (level <= 0) return
        if (!cooldown.getOrDefault(player.uniqueId, Cooldown(0)).hasExpired() || !player.isSneaking) return

        cooldown[player.uniqueId] = Cooldown((if (level >= 3) 25 else 120 - (level * 30)).toLong(), TimeUnit.SECONDS)
        event.projectile.setMetadata("ender_bow", FixedMetadataValue(Main.instance, true))
    }

    @EventHandler
    fun onBowHit(event: ProjectileHitEvent) {
        if (!event.entity.hasMetadata("ender_bow") || event.entity.shooter == null) return

        val player = event.entity.shooter as? Player ?: return
        val arrow = event.entity as? Arrow ?: return

        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return
        val arrowLoc = arrow.location.clone().apply {
            yaw = player.location.yaw
            pitch = player.location.pitch
        }

        player.teleport(arrowLoc)

        LocationUtil.getNearestPlayer(arrow, 10.0)?.let { target ->
            if (target == player) return@let
            val targetLoc = target.location
            val arrowPos = arrowLoc.toVector()
            val targetPos = targetLoc.toVector()

            val toTarget = targetPos.subtract(arrowPos)
            val dx = toTarget.x
            val dy = toTarget.y
            val dz = toTarget.z
            val horizontalDist = sqrt(dx * dx + dz * dz)

            val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f
            val pitch = Math.toDegrees(atan2(-dy, horizontalDist)).toFloat()

            arrowLoc.apply {
                this.yaw = normalizeAngle(yaw)
                this.pitch = pitch
            }
        } ?: return

        player.teleport(arrowLoc)
    }

    fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0) normalized += 360f
        return normalized
    }

    override fun getText(p0: Int, player: Player): String {
        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return "&c&l✘"
        return getCooldownActionText(cooldown[player.uniqueId] ?: Cooldown(0L))
    }
}