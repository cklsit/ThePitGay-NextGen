package cn.irina.thepitaddon.enchantment.type.recode.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.chat.RomanUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.random.RandomUtil
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@ArmorOnly
class DivineMiracle : AbstractEnchantment(),  IPlayerDamaged, Listener {
    private val isActive = HashMap<UUID, Boolean>()

    override fun getEnchantName(): String {
        return "奇迹立场"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "divine_miracle_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7受到致命伤害时有 &d+" + enchantLevel * 15 + "% &7几率免疫该次伤害 (死亡前只可触发一次), 恢复 &c" + enchantLevel.toDouble() * 2 + "❤ /s" +
                "&7同时获得效果 &3抗性提升 " + RomanUtil.convert(enchantLevel) + " &f(00:04) /s" +
                "&7(死亡时有 &d+" + enchantLevel * 15 + "% &7的几率不损失背包内神话物品生命)"
    }

    override fun handlePlayerDamaged(
        enchantLevel: Int,
        victim: Player,
        entity: Entity,
        damage: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        cancel: AtomicBoolean
    ) {
        if (isActive.getOrDefault(victim.uniqueId, false)) return

        if (victim.health - damage > 1 || !RandomUtil.hasSuccessfullyByChance(enchantLevel * 0.15)) return
        isActive[victim.uniqueId] = true
        cancel.set(true)

        PlayerUtil.heal(victim, (enchantLevel * 4).toDouble())
        if (victim.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) victim.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE)
        victim.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 4, enchantLevel - 1, false, true))
        victim.sendMessage(CC.translate("&d&l奇迹立场! &7你免疫了一次致命攻击!"))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (!isActive.getOrDefault(player.uniqueId, false)) return

        isActive.remove(player.uniqueId)
    }
}
