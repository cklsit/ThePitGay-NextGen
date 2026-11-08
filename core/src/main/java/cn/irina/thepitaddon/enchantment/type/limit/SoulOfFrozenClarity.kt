package cn.irina.thepitaddon.enchantment.type.limit

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.parm.listener.ITickTask
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/*
 * @Author Irina
 * @Date 2025/8/14 17:24
 */

@ArmorOnly
class SoulOfFrozenClarity: AbstractEnchantment(), ITickTask, IPlayerDamaged, Listener {
    private val pitApi = ThePit.getApi()
    private val instance = Main.instance

    override fun getEnchantName(): String {
        return "冰心玉魄"
    }

    override fun getMaxEnchantLevel(): Int {
        return 1
    }

    override fun getNbtName(): String {
        return "soul_of_frozen_clarity"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(i: Int): String {
        return "&7穿戴含有此附魔的 &e神话之甲 &7时, 将获得以下效果: /s" +
                "   &f❖ &b流玉环绕 /s" +
                "   &f❖ &3寒音反震 /s" +
                "   &f❖ &1冰魄之盾 /s" +
                "&7效果 &b流玉环绕 &f: &7自身永久获得 &3抗性提升 &fIII /s" +
                "&7效果 &1冰魄之盾 &f: &7每间隔 &e15s &7时将获得一层可以抵消伤害的护盾, (最高 &e3 &7层) /s" +
                "护盾破裂将恢复为最大生命值 &c20% &7的血量 /s" +
                "&7效果 &3寒音反震 &f: &7受到普通致命伤害时, 将反弹 &c65% &7的普通伤害, /s" +
                "同时, 获得效果 &c生命恢复 V &f(00:02) (死亡前只可触发一次)"
    }

    private val shieldMap = ConcurrentHashMap<UUID, Int>()
    private val healEffect = PotionEffect(PotionEffectType.REGENERATION, 40, 4, true, false)
    private val hasBlocked = ConcurrentHashMap<UUID, Boolean>()
    override fun handlePlayerDamaged(level: Int, victim: Player, entity: Entity, damage: Double, p4: AtomicDouble, p5: AtomicDouble, cancel: AtomicBoolean) {
        val attacker = entity as? Player ?: return
        val uuid = victim.uniqueId
        val map = shieldMap[uuid] ?: 0
        if (map >= 1) {
            shieldMap[uuid] = map - 1

            val backHealth = victim.maxHealth * 0.2
            PlayerUtil.heal(victim, backHealth)

            victim.sendMessage(CC.translate("&b&l冰心玉魄 &7你抵挡了一次伤害, 护盾损失一层, 当前剩余 &e${shieldMap[uuid]} &7层"))
            attacker.sendMessage(CC.translate("&b&l冰心玉魄 &7目标抵挡了一次伤害, 护盾损失一层!"))
            victim.world.playSound(victim.location, Sound.ANVIL_BREAK, 0.5f, 0.5f)
        }

        val currentHealth = victim.health
        val blockMap = hasBlocked[uuid] ?: false
        if (blockMap || currentHealth - damage > 0.0) return
        hasBlocked[uuid] = true

        cancel.set(true)

        victim.addPotionEffect(healEffect)

        val backDamage = damage * 0.65
        PlayerUtil.damage(victim, attacker, PlayerUtil.DamageType.NORMAL, backDamage, true)
    }

    @EventHandler
    fun onDeath(evt: PlayerDeathEvent) {
        val victim = evt.entity
        val uuid = victim.uniqueId

        val map = hasBlocked[uuid] ?: return
        if (map) hasBlocked.remove(uuid)
    }

    private val effectType: PotionEffectType = PotionEffectType.DAMAGE_RESISTANCE
    private val effect = PotionEffect(effectType, Int.MAX_VALUE, 2, true, false)
    private val runnableMap = ConcurrentHashMap<UUID, BukkitTask>()
    override fun handle(level: Int, player: Player) {
        if (player.hasPotionEffect(effectType)) player.removePotionEffect(effectType)
        player.addPotionEffect(effect)

        val uuid = player.uniqueId
        if (runnableMap[uuid] != null) return

        val task = object: BukkitRunnable() {
            val map = shieldMap[uuid] ?: 0

            override fun run() {
                if (map < 3) shieldMap[uuid] = map + 1

                val level = pitApi.getItemEnchantLevel(player.inventory.leggings, nbtName)
                if (level > 0) return
                runnableMap.remove(uuid)

                this.cancel()
                return
            }
        }.runTaskTimerAsynchronously(instance, 20L, 0)

        runnableMap[uuid] = task
    }

    override fun loopTick(p0: Int): Int { return 20 }
}