package cn.irina.thepitaddon.enchantment.type.limit

import cn.irina.thepitaddon.Main
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.ITickTask
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/*
 * @Author Irina
 * @Date 2025/10/4 21:40
 */

class LivelyParty: AbstractEnchantment(), ITickTask, IActionDisplayEnchant {

    override fun getEnchantName(): String = "热闹派对"
    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.OP
    override fun getCooldown(): Cooldown? = null
    override fun getMaxEnchantLevel(): Int = 3
    override fun getNbtName(): String = "lively_party"

    override fun getUsefulnessLore(i: Int): String {
        return "&7穿戴附有此附魔的 &e神话之甲 &7时 /s" +
                "&7以自身为中心范围 &e5格 &7内有其他玩家时 /s" +
                "&7每秒自身将额外恢复 &c0.5❤ &7血量 (上限${i * 4}人)"
    }

    companion object {
        const val RADIUS = 5.0
        const val MAX_TARGETS = 4
    }

    private val targets = ConcurrentHashMap<UUID, Int>()
    private fun getTargets(player: Player): Int = targets.getOrPut(player.uniqueId) { 0 }

    private val instance = Main.instance
    override fun handle(i: Int, p: Player) {
        var radiusPlayers: Int? = 0
        Bukkit.getScheduler().runTaskAsynchronously(instance) {
            radiusPlayers = p.getNearbyEntities(RADIUS, RADIUS, RADIUS)
                .filterIsInstance<Player>()
                .filter { it != p }
                .toList()
                .size

            targets[p.uniqueId] = radiusPlayers!!
        }

        val maxCount = i * MAX_TARGETS
        if (radiusPlayers == null || radiusPlayers == 0) return

        val count = radiusPlayers!!.coerceAtMost(maxCount)
        PlayerUtil.heal(p, count.toDouble())
    }

    override fun loopTick(p0: Int): Int = 20

    override fun getText(i: Int, p: Player): String {
        val maxCount = i * MAX_TARGETS
        val currentTargets = getTargets(p)
        return currentTargets.coerceAtMost(maxCount).toString()
    }
}