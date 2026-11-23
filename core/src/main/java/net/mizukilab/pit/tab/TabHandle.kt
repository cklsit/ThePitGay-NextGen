package net.mizukilab.pit.tab

import cn.charlotte.pit.ThePit
import me.clip.placeholderapi.PlaceholderAPI
import net.mizukilab.pit.config.TabConfiguration.animation
import net.mizukilab.pit.config.TabConfiguration.delay
import net.mizukilab.pit.config.TabConfiguration.head
import net.mizukilab.pit.config.TabConfiguration.part
import net.mizukilab.pit.config.TabConfiguration.tick
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * @author Araykal
 * @since 2025/5/12
 */
open class TabHandle {
    var counter = 0
    private data class SentTab(val player: Player, val header: String, val footer: String)
    private val lastSent = HashMap<UUID, SentTab>()

    fun fetchTab() {
        object : BukkitRunnable() {
            override fun run() {
                updateTabListForAllPlayers()
                counter++
            }
        }.runTaskTimer(
            ThePit.getInstance(),
            delay.coerceAtLeast(0).toLong(),
            tick.coerceAtLeast(1).toLong()
        )
    }

    private fun updateTabListForAllPlayers() {
        updatePlayers(Bukkit.getOnlinePlayers())
    }

    private fun updatePlayers(players: Collection<Player>) {
        if (players.isEmpty()) {
            lastSent.clear()
            return
        }
        val onlineIds = players.mapTo(HashSet()) { it.uniqueId }
        lastSent.keys.retainAll(onlineIds)
        val currentValues = mutableMapOf<String, String>()
        for ((key, values) in animation) {
            if (values.isNotEmpty()) {
                currentValues["{$key}"] = values[Math.floorMod(counter, values.size)]
            }
        }
        // Only animation and common formatting are shared. PAPI remains per player.
        val headerText = replaceVariables(head, currentValues).replace("&", "§").replace("/s", "§r")
        val footerText = replaceVariables(part, currentValues).replace("&", "§").replace("/s", "§r")
        for (player in players) {
            updatePlayerTabList(player, headerText, footerText)
        }
    }

    private fun updatePlayerTabList(player: Player, headerText: String, footerText: String) {
        try {
            val papiReady = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
            var header = if (papiReady && '%' in headerText) PlaceholderAPI.setPlaceholders(player, headerText) else headerText
            var footer = if (papiReady && '%' in footerText) PlaceholderAPI.setPlaceholders(player, footerText) else footerText
            // 未装 PlaceholderAPI 时也要让 %pit_xxx% 生效，否则 TAB 会原样显示占位符
            if ('%' in header) header = net.mizukilab.pit.hook.PitPlaceholders.apply(player, header)
            if ('%' in footer) footer = net.mizukilab.pit.hook.PitPlaceholders.apply(player, footer)
            val previous = lastSent[player.uniqueId]
            if (previous?.player === player && previous.header == header && previous.footer == footer) return

            sendTabPacket(player, header, footer)
            // Only successful sends are remembered; reconnects always receive an initial packet.
            lastSent[player.uniqueId] = SentTab(player, header, footer)
        } catch (e: Exception) {
            player.sendMessage("Error: §${e.message}")
            e.printStackTrace()
        }
    }

    protected open fun sendTabPacket(player: Player, header: String, footer: String) {
        // 用 Bukkit/Adventure 直发，避免依赖 ProtocolLib（未安装时本类会加载失败）
        player.sendPlayerListHeaderAndFooter(
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(header),
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(footer)
        )
    }

    private fun replaceVariables(lines: List<String>, variables: Map<String, String>): String {
        val builder = StringBuilder()
        for (line in lines) {
            var modifiedLine = line
            for ((key, value) in variables) {
                modifiedLine = modifiedLine.replace(key, "$value&r")
            }
            builder.append(modifiedLine).append("\n")
        }
        return builder.toString().trim()
    }
}