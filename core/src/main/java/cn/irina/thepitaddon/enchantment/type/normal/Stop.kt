package cn.irina.thepitaddon.enchantment.type.normal

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@BowOnly
class Stop : AbstractEnchantment(), IPlayerShootEntity {
    companion object {
        private val reduceSpeedBuffCooldown = mutableMapOf<UUID, Cooldown>()
    }

    override fun getEnchantName(): String {
        return "停下!"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "please_stop"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        var cd = 4 - enchantLevel
        val duration = enchantLevel * 2 + 2
        val s = StringBuilder("&7弓箭命中敌人后对敌人施加 &c缓慢 I &f(00:0${duration}) /s")
        if (enchantLevel >= 2) {
            s.append("&7同时, 降低敌人的一级 &b速度 &7效果")
            s.append(" (${cd}s冷却)")
            s.append(".")
        }
        return s.toString()
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity?,
        damage: Double,
        finalDamage: AtomicDouble?,
        boostDamage: AtomicDouble?,
        cancel: AtomicBoolean?
    ) {
        if (target !is Player) return
        if (target.activePotionEffects.find { it.type == PotionEffectType.SLOW } != null)
            target.removePotionEffect(
                PotionEffectType.SLOW
            )
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOW, (enchantLevel * 2 + 2) * 20, 0))
        var cd = 4 - enchantLevel
        val cooldown = reduceSpeedBuffCooldown[attacker.uniqueId]
        if (enchantLevel >= 3) {
            reduceSpeedBuff(target)
            reduceSpeedBuffCooldown[attacker.uniqueId] = Cooldown(cd * 1000L)
        } else if (cooldown == null || cooldown.hasExpired()) {
            reduceSpeedBuff(target)
            reduceSpeedBuffCooldown[attacker.uniqueId] = Cooldown(cd * 1000L)
        }
    }

    private fun reduceSpeedBuff(targetPlayer: Player) {
        val existingSpeed = targetPlayer.activePotionEffects.find { it.type == PotionEffectType.SPEED }
        if (existingSpeed == null) return
        if (existingSpeed.amplifier == 0) {
            targetPlayer.removePotionEffect(PotionEffectType.SPEED)
            return
        }
        var potionEffectTime = existingSpeed.duration
        var potionEffectLevel = existingSpeed.amplifier
        potionEffectLevel -= 1
        targetPlayer.removePotionEffect(PotionEffectType.SPEED)
        targetPlayer.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED,
                potionEffectTime,
                potionEffectLevel,
                false,
                true
            )
        )
        targetPlayer.sendMessage(CC.translate("&c&l停下! &7你的速度等级降低了."))
    }
}