package cn.irina.thepitaddon.enchantment.type.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.chat.RomanUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2

@BowOnly
class BackStab : AbstractEnchantment(),  IPlayerShootEntity, IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "背刺"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "back_stab"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7命中目标后, 将立刻传送至目标的身后 (" + (if (enchantLevel >= 3) 10 else 20) + "s冷却) /s" +
                "&7同时, 将立刻获得 &b速度 " + RomanUtil.convert(enchantLevel) + " &f(00:0" + (enchantLevel + 1) + ")"
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
        if (entity !is Player || !Companion.cooldown.getOrDefault(shooter.getUniqueId(), Cooldown(0L))!!
                .hasExpired()
        ) return
        Companion.cooldown.put(
            shooter.uniqueId,
            Cooldown((if (enchantLevel >= 3) 10 else 20).toLong(), TimeUnit.SECONDS)
        )
        val target = entity

        teleportBehind(shooter, target)

        for (effect in shooter.activePotionEffects) {
            if (effect.type != PotionEffectType.SPEED) continue

            if (effect.amplifier >= enchantLevel - 1) continue

            shooter.removePotionEffect(PotionEffectType.SPEED)
            shooter.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SPEED,
                    (enchantLevel + 1) * 20,
                    enchantLevel - 1,
                    false,
                    true
                )
            )
            return
        }
        shooter.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED,
                (enchantLevel + 1) * 20,
                enchantLevel - 1,
                false,
                true
            )
        )
    }

    override fun getText(i: Int, player: Player): String? {
        return getCooldownActionText(Companion.cooldown.getOrDefault(player.getUniqueId(), Cooldown(0L)))
    }

    companion object {
        private val cooldown = HashMap<UUID?, Cooldown?>()

        fun teleportBehind(player: Player, target: Player) {
            val targetLocation = target.location
            val targetDirection = targetLocation.direction.normalize()

            targetDirection.setY(0)
            targetDirection.normalize()

            val behindLocation = targetLocation.clone().subtract(targetDirection.multiply(0.5))

            player.teleport(behindLocation)

            val playerLocation = player.location
            val deltaX = targetLocation.x - playerLocation.x
            val deltaZ = targetLocation.z - playerLocation.z

            val yaw = (atan2(deltaZ, deltaX) * (180 / Math.PI)).toFloat() - 90

            playerLocation.yaw = yaw
            playerLocation.pitch = 0f
            player.teleport(playerLocation)
        }
    }
}
