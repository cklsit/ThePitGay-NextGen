package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.Effect
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@ArmorOnly
class Unpredictably : AbstractEnchantment(),  IAttackEntity, IPlayerDamaged, IPlayerKilledEntity,
    IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "强力击: 眩晕"
    }

    override fun getMaxEnchantLevel(): Int {
        return 1
    }

    override fun getNbtName(): String {
        return "unpredictably_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.DARK_RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(i: Int): String {
        return "&7穿着时自身受到的伤害 &9-20%&7, 同时造成的伤害 &c-20% &7/s" +
                "&7每 &e4 &7次命中目标将造成大量击退, 并施加效果 &8眩晕 &f(00:02) &7效果 (20s冷却) /s" +
                "&7效果 &8眩晕&f: &7短时间内使目标 &b移速 &7与 &c攻击伤害 &7大幅度降低 /s" +
                "&7(每击杀一位目标将减少该附魔2s冷却时长)"
    }

    override fun handleAttackEntity(
        i: Int,
        attacker: Player,
        entity: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean?
    ) {
        if (entity !is Player) return
        boostDamage.set(boostDamage.get() * 0.8)

        if (!Companion.cooldown.getOrDefault(attacker.uniqueId, Cooldown(0L))!!.hasExpired()) return

        val hit = PlayerProfile.getRawCache(attacker.uniqueId).meleeHit

        if (hit % 4 != 0) return
        Companion.cooldown.put(attacker.uniqueId, Cooldown(20L, TimeUnit.SECONDS))
        val victim = entity
        if (victim.hasPotionEffect(PotionEffectType.SLOW)) victim.removePotionEffect(PotionEffectType.SLOW)
        if (victim.hasPotionEffect(PotionEffectType.WEAKNESS)) victim.removePotionEffect(PotionEffectType.WEAKNESS)
        victim.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 2 * 20, 3, false, true))
        victim.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 2 * 20, 3, false, true))

        val direction = victim.location.toVector().subtract(attacker.location.toVector()).normalize()

        val horizontalStrength = 2.0
        val verticalStrength = 1.5

        val kb = direction.multiply(horizontalStrength)
        kb.setY(verticalStrength)

        victim.velocity = kb

        val location = victim.location

        victim.world.playSound(location, Sound.EXPLODE, 2f, 2f)
        victim.world.playEffect<Any?>(location, Effect.EXPLOSION_LARGE, null)
    }

    override fun handlePlayerDamaged(
        i: Int,
        player: Player?,
        entity: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        reduceDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean?
    ) {
        reduceDamage.set(reduceDamage.get() * 0.8)
    }

    override fun handlePlayerKilled(
        i: Int,
        killer: Player,
        entity: Entity?,
        atomicDouble: AtomicDouble?,
        atomicDouble1: AtomicDouble?
    ) {
        if (Companion.cooldown.get(killer.uniqueId)!!.hasExpired()) return
        Companion.cooldown.put(
            killer.uniqueId, Cooldown(
                max(0.0, (Companion.cooldown.get(killer.uniqueId)!!.remaining - 2000L).toDouble()).toLong()
            )
        )
    }

    override fun getText(i: Int, player: Player): String? {
        return if (Companion.cooldown.getOrDefault(player.uniqueId, Cooldown(0L))!!.hasExpired())
            getHitActionText(player, 4)
        else
            getCooldownActionText(Companion.cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }

    companion object {
        private val cooldown = HashMap<UUID?, Cooldown?>()
    }
}
