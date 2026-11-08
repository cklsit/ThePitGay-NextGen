package cn.irina.thepitaddon.enchantment.type.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.Main
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Chicken
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@BowOnly
class DevilChicken : AbstractEnchantment(), IPlayerShootEntity, IActionDisplayEnchant {
    private val cooldown = HashMap<UUID, Cooldown>()

    override fun getEnchantName(): String {
        return "恶魔鸡"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "devil_chicken"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7射出的箭矢命中玩家时将召唤 &e" + enchantLevel + " &7只 &4恶魔鸡 &7并在 &e1s &7后爆炸&7, /s" +
                "每只鸡将对周围 &e2格 &7内的玩家造成最多 &f" + enchantLevel.toDouble() + "❤ &7的&f真实&7伤害 &7(" + (if (enchantLevel >= 3) 1 else 4 - enchantLevel) + "s冷却)"
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        attacker: Player,
        entity: Entity,
        damage: Double,
        finalDamage: AtomicDouble,
        boostDamage: AtomicDouble,
        cancel: AtomicBoolean
    ) {
        if (entity !is Player) return
        if (cooldown.getOrDefault(attacker.uniqueId, Cooldown(0L)).hasExpired()) {
            cooldown[attacker.uniqueId] =
                Cooldown(
                    (if (enchantLevel >= 3) 1 else 4 - enchantLevel).toLong(),
                    TimeUnit.SECONDS
                )
            spawnChickens(entity.location, enchantLevel)
        }
    }

    private fun spawnChickens(location: Location, enchantLevel: Int) {
        for (i in 0..< enchantLevel) {
            val spawnLocation = location.clone().add(Math.random() * 4 - 2, 1.0, Math.random() * 4 - 2)

            val chicken = location.world.spawn(spawnLocation, Chicken::class.java)
            chicken.customName = CC.translate("&4恶魔鸡")
            chicken.isCustomNameVisible = true
            chicken.maxHealth = 2000.0
            chicken.health = 2000.0

            object : BukkitRunnable() {
                override fun run() {
                    customExplosion(chicken.location)
                    chicken.remove()
                }
            }.runTaskLater(Main.instance, 14L)
        }
    }

    private fun customExplosion(location: Location) {
        for (entity in location.world.getNearbyEntities(location, 3.0, 3.0, 3.0)) {
            if (entity !is Player) continue

            PlayerUtil.damage(entity, PlayerUtil.DamageType.TRUE, 2.0, true)

            entity.world.playSound(location, Sound.EXPLODE, 2f, 2f)
            entity.world.playEffect<Any?>(location, Effect.EXPLOSION_LARGE, null)

            val currentVelocity = entity.velocity
            entity.velocity = Vector(currentVelocity.x, 0.5, currentVelocity.z)
        }
    }

    override fun getText(level: Int, attacker: Player): String {
        if (!PlayerUtil.isVenom(attacker)) return getCooldownActionText(
            cooldown.getOrDefault(
                attacker.uniqueId,
                Cooldown(0L)
            )
        )

        return "&c&l✘"
    }
}
