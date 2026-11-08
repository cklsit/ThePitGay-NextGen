package cn.irina.thepitaddon.enchantment.type.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.Main
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

@BowOnly
@WeaponOnly
class BrokenString : AbstractEnchantment(),  IPlayerShootEntity, IAttackEntity, Listener, IActionDisplayEnchant {
    private val brokenString = "BrokenString"
    private val cooldown = HashMap<UUID, Cooldown>()

    override fun getEnchantName(): String {
        return "断弦"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "Combo_Broken_String"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7命中目标时对目标施加 &8断弦 &7(00:02) 效果 (${30 - (enchantLevel * 5)}s冷却) /s" +
                "&7效果 &8断弦&7: 无法射出箭矢"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (target !is Player) return

        if (!cooldown.getOrDefault(attacker.uniqueId, Cooldown(0L)).hasExpired()) return
        cooldown[attacker.uniqueId] = Cooldown(30L - (enchantLevel * 5), TimeUnit.SECONDS)

        onActive(attacker, target)

    }

    override fun handleShootEntity(
        enchantLevel: Int,
        shooter: Player,
        target: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (target !is Player) return

        if (!cooldown.getOrDefault(shooter.uniqueId, Cooldown(0L)).hasExpired()) return
        cooldown[shooter.uniqueId] = Cooldown(30L - (enchantLevel * 5), TimeUnit.SECONDS)

        onActive(shooter, target)

    }

    @EventHandler
    fun onShoot(event: EntityShootBowEvent) {
        if (event.entity !is Player) return
        val shooter = event.entity as Player

        if (!shooter.hasMetadata(brokenString) || (shooter.getMetadata(brokenString)[0]).asLong() <= System.currentTimeMillis()) return

        event.isCancelled = true
        shooter.sendMessage(CC.translate("&c&l断弦! &7你现在无法发射箭矢!"))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (player.hasMetadata(brokenString)) player.removeMetadata(brokenString, Main.instance)
    }

    private fun onActive(attacker: Player, targetPlayer: Player) {
        if (targetPlayer.hasMetadata(brokenString)) targetPlayer.removeMetadata(brokenString, Main.instance)

        targetPlayer.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW_DIGGING,
                2 * 20,
                0,
                false,
                false
            )
        )
        targetPlayer.setMetadata(
            brokenString,
            FixedMetadataValue(Main.instance, System.currentTimeMillis() + (2 * 1000L))
        )

        attacker.sendMessage(CC.translate("&c&l断弦! &f" + targetPlayer.displayName + " &7将在接下来 &e2s &7内无法发射箭矢!"))
        targetPlayer.sendMessage(CC.translate("&c&l断弦! &7你将在接下来 &e2s &7内无法发射箭矢!"))
    }


    override fun getText(i: Int, player: Player): String {
        return getCooldownActionText(cooldown.getOrDefault(player.uniqueId, Cooldown(0L)))
    }
}
