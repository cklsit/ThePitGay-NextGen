package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import cn.charlotte.pit.event.PitStreakKillChangeEvent
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤收尾条目：100 归砧。
 *
 * 连杀结束时额外保留 live；下一次神话之井重铸后的首击附带一层锤印的等效加成。
 */
class HammerMiscHooks : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onStreakChange(event: PitStreakKillChangeEvent) {
        // 只在连杀归零（结束）时结算
        if (event.to > 0.0 || event.from <= 0.0) return
        val profile = event.playerProfile ?: return
        val player = Bukkit.getPlayer(profile.playerUuid) ?: return
        val mythic = hammerOf(player) ?: return
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && (it.key as HammerEnchantBase).nbtName == "hammer_100" && SeasonalOpenService.isOpen(it.key)
        } ?: return

        val base = entry.key as HammerEnchantBase
        val keep = base.valueAt(entry.value).toInt().coerceAtLeast(1)
        val max = mythic.maxLive
        if (max > 0) {
            val next = (mythic.live + keep).coerceAtMost(max)
            if (next != mythic.live) {
                mythic.live = next
                player.inventory.setItemInMainHand(mythic.toItemStack())
                player.sendMessage(CC.translate("&a&l归砧! &7连杀结束, 额外保留了 &a$keep &7点生命."))
            }
        }
        pendingFirstHit.add(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onFirstHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        if (!pendingFirstHit.remove(attacker.uniqueId)) return
        val mythic = hammerOf(attacker) ?: return
        event.damage = event.damage * 1.08
        attacker.world.spawnParticle(Particle.ENCHANTED_HIT, event.entity.location.add(0.0, 1.0, 0.0), 12, 0.3, 0.3, 0.3, 0.0)
        attacker.playSound(attacker.location, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onQuit(event: PlayerQuitEvent) {
        pendingFirstHit.remove(event.player.uniqueId)
    }

    private fun hammerOf(player: Player): IMythicItem? {
        val item = player.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return null
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return null
        return if (mythic.enchantments.isEmpty()) null else mythic
    }

    companion object {
        private val pendingFirstHit = ConcurrentHashMap.newKeySet<UUID>()
    }
}
