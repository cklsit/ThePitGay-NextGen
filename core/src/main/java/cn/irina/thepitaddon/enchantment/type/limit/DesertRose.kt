package cn.irina.thepitaddon.enchantment.type.limit

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.manager.PitManager
import cn.irina.thepitaddon.utils.TimeUtil
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.Utils
import net.mizukilab.pit.util.chat.RomanUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Author: ShanguanLinG
 * @Date: 2025/9/23 20:32
 */

@BowOnly
class DesertRose : AbstractEnchantment(), IPlayerShootEntity, Listener, IActionDisplayEnchant {

    companion object {
        private val cooldown: HashMap<UUID, Cooldown> = HashMap()
        private val redLaserCooldown: HashMap<UUID, Cooldown> = HashMap()
        private val blueLaserCooldown: HashMap<UUID, Cooldown> = HashMap()
    }

    private val pitApi = ThePit.getApi()

    override fun getEnchantName(): String {
        return "沙漠玫瑰"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "desert_rose"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }


    override fun getUsefulnessLore(enchantLevel: Int): String {
        val slownessBuffLevel = RomanUtil.convert(1)
        val slownessBuffDuration = TimeUtil.formatTotalSeconds(enchantLevel * 2 + 2)
        val weaknessBuffLevel = RomanUtil.convert(enchantLevel + 1)
        val weaknessBuffDuration = TimeUtil.formatTotalSeconds(enchantLevel * 2 + 2)
        val speedBuffLevel = RomanUtil.convert(enchantLevel + 1)
        val speedDuration = TimeUtil.formatTotalSeconds(enchantLevel * 2 + 4)
        return "&7射箭时无需蓄力即可让箭矢以最大蓄力状态射出, 若命中目标则召唤一道激光./s" +
                "&7激光命中敌人时, 将会产生一次爆炸并对周围敌人施加 &c虚弱 ${weaknessBuffLevel} &f($weaknessBuffDuration) &7与 &c缓慢 ${slownessBuffLevel} &f($slownessBuffDuration)/s" +
                "&7同时, 若爆炸接触目标生命值低于 &c${enchantLevel * 0.5 + 0.5}❤ &7时, 将直接致死. /s" +
                "&7若射箭时处于潜行状态则直接发射激光, 命中目标时, 将自身传送至爆炸处并获得 &b速度 ${speedBuffLevel} &f($speedDuration) &7(15秒冷却) /s" +
                "&7此附魔每秒只能触发一次./s" + "/s  \"&7&o穿越过去与未来我随意游走\""
    }

    @EventHandler
    fun onInteract(event: EntityShootBowEvent) {
        if (event.entity !is Player) return
        if (event.force >= 1) return
        val player = event.entity as Player
        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return
        val itemInHand = player.itemInHand ?: return
        val level = pitApi.getItemEnchantLevel(player.itemInHand, this.nbtName)
        if (level == -1) {
            return
        }
        if (itemInHand.type != Material.BOW) return
        if (!Companion.cooldown.getOrDefault(player.uniqueId, Cooldown(0)).hasExpired()) return
        Companion.cooldown[player.uniqueId] = Cooldown(1, TimeUnit.SECONDS)
        if ((event.entity as Player).isSneaking) {
            if (!Companion.blueLaserCooldown.getOrDefault(player.uniqueId, Cooldown(0)).hasExpired()) return
            val direction = player.location.direction.normalize()
            shootLaser(player, level, player.eyeLocation, direction, 102f, 204f, 255f)
            event.isCancelled = true
            return
        }
        val arrow = player.launchProjectile(org.bukkit.entity.Arrow::class.java)
        arrow.velocity = player.location.direction.multiply(3.0)
        event.isCancelled = true
    }

    private fun shootLaser(
        player: Player,
        level: Int,
        startLocation: Location,
        direction: org.bukkit.util.Vector,
        r: Float,
        g: Float,
        b: Float
    ) {
        val laserLength = (level * 10 + 10) * 2
        val maxIterations = if (player.isSneaking) laserLength * 2 else laserLength
        for (i in 0..maxIterations) {
            val particleLocation = startLocation.clone().add(direction.clone().multiply(i * 0.5))
            Bukkit.getOnlinePlayers()
                .forEach { targetPlayer -> sendRedstoneParticle(targetPlayer, particleLocation, r, g, b) }
            val nearbyEntities = particleLocation.world.getNearbyEntities(particleLocation, 0.5, 0.5, 0.5)
            for (entity in nearbyEntities) {
                if (entity !is Player || entity == player || entity.isDead || entity.location.y > 100) continue
                entity.noDamageTicks = 0
                val targetLoc = entity.location.clone().add(0.0, 0.5, 0.0)
                applyLaserEffects(player, entity, targetLoc, level)
                execution(player, entity, targetLoc, level)
                vampireWithPerk(player)
                redLaserCooldown[player.uniqueId] = Cooldown(1, TimeUnit.SECONDS)
                if (player.isSneaking) {
                    if (!Companion.blueLaserCooldown.getOrDefault(player.uniqueId, Cooldown(0)).hasExpired()) return
                    player.teleport(entity.location.clone())
                    blueLaserCooldown[player.uniqueId] = Cooldown(15, TimeUnit.SECONDS)
                    PitManager.givePlayerPotionEffect(player, PotionEffectType.SPEED,level * 2 * 20 + 4 * 20, level)
                    blueLaserCooldown[player.uniqueId] = Cooldown(15, TimeUnit.SECONDS)
                }
                return
            }
        }
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity,
        damage: Double,
        finalDamage: AtomicDouble?,
        boostDamage: AtomicDouble?,
        cancel: AtomicBoolean?
    ) {
        if (!Companion.redLaserCooldown.getOrDefault(attacker.uniqueId, Cooldown(0)).hasExpired()) return
        if (target !is Player) return
        val direction = target
            .eyeLocation
            .clone()
            .add(0.0, 0.5, 0.0)
            .toVector()
            .subtract(attacker.eyeLocation.toVector()).normalize()
        shootLaser(attacker, enchantLevel, attacker.eyeLocation, direction, 255f, 0f, 0f)
    }

    private fun vampireWithPerk(player: Player) {
        if (PlayerUtil.isPlayerChosePerk(player, "Vampire")) {
            PlayerUtil.heal(player, 3.0)
        }
    }

    private fun execution(attacker: Player, target: Player, location: Location, level: Int) {
        val nearbyPlayers = location.world.getNearbyEntities(location, 4.0, 4.0, 4.0)
        for (entity in nearbyPlayers) {
            if (entity !is Player || entity == target || entity == attacker || entity.isDead) continue
            val healthThreshold = level * 0.5 + 0.5
            if (entity.health <= healthThreshold * 2) {
                PlayerUtil.damage(
                    attacker,
                    entity,
                    PlayerUtil.DamageType.TRUE,
                    healthThreshold * 2,
                    false
                )
                attacker.playSound(attacker.location, Sound.ENTITY_VILLAGER_DEATH, 1f, 0.5f)
                val deathLoc = entity.location
                deathLoc.world.playEffect(deathLoc, Effect.STEP_SOUND, Material.REDSTONE_BLOCK)
                deathLoc.world.playEffect(deathLoc.clone().subtract(0.0, 1.0, 0.0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK)
            }
        }
    }

    private fun applyLaserEffects(attacker: Player, target: Player, location: Location, level: Int) {
        val damage = 1.0
        target.damage(damage)
        location.world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
        location.world.spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, location, 1, 0.0, 0.0, 0.0, 0.0)
        val nearbyPlayers = location.world.getNearbyEntities(location, 4.0, 4.0, 4.0)
        for (entity in nearbyPlayers) {
            if (entity !is Player || entity == target || entity == attacker || entity.isDead) continue
            entity.damage(damage)
            setVector(entity, location)
            applyEffect(entity, level)
        }
    }

    private fun applyEffect(target: Player, level: Int) {
        if (target.hasPotionEffect(PotionEffectType.SLOWNESS)) target.removePotionEffect(PotionEffectType.SLOWNESS)
        if (target.hasPotionEffect(PotionEffectType.WEAKNESS)) target.removePotionEffect(PotionEffectType.WEAKNESS)
        target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * level + 20 * 2, 0))
        target.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 20 * level + 20 * 2, level))
    }

    private fun setVector(
        entity: Entity,
        location: Location,
        x: Double = 0.6,
        y: Double = 0.4
    ) {
        val vector = entity.location.toVector().subtract(location.toVector()).normalize()
        vector.multiply(x)
        vector.setY(y)
        entity.velocity = vector
    }

    private fun sendRedstoneParticle(sender: Player, location: Location, r: Float, g: Float, b: Float) {
        sender.spawnParticle(
            org.bukkit.Particle.DUST,
            location,
            1,
            0.0,
            0.0,
            0.0,
            0.0,
            org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(r.toInt(), g.toInt(), b.toInt()), 1.0f)
        )
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        Companion.cooldown.remove(e.player.uniqueId)
        Companion.redLaserCooldown.remove(e.player.uniqueId)
        Companion.blueLaserCooldown.remove(e.player.uniqueId)
    }

    override fun getText(level: Int, player: Player): String {
        return this.getCooldownActionText(
            Companion.redLaserCooldown.getOrDefault(
                player.uniqueId,
                Cooldown(0L)
            )
        ) + " &7| " + this.getCooldownActionText(
            Companion.blueLaserCooldown.getOrDefault(
                player.uniqueId,
                Cooldown(0L)
            )
        )
    }

    override fun getCooldownActionText(cooldown: Cooldown): String {
        return if (cooldown.hasExpired()) "&a&l✔" else "&c&l" + net.mizukilab.pit.util.time.TimeUtil.millisToRoundedTime(
            cooldown.remaining
        ).replace(" ", "")
    }
}