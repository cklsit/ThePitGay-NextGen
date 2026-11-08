package cn.irina.thepitaddon.command.player

import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.data.KillerData
import cn.irina.thepitaddon.utils.TimeUtil
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/*
 * @Author Irina
 * @Date 2025/8/22 20:29
 */

@Command(name = "whenDidIDie")
class GetLastestKiller : Listener {
    private val prefix = Main.instance.PREFIX
    private val killerData = ConcurrentHashMap<UUID, KillerData>()

    @Execute
    fun onCommand(@Context player: Player) {
        val map = killerData[player.uniqueId] ?: return
        val time = TimeUtil.formatTimestamp(map.killTime)
        val name = map.killerName

        player.sendMessage(CC.translate("$prefix&7您最新一次死亡于 &e$time &7被目标 &e$name &7击杀!"))
    }

    @EventHandler
    fun onDeath(evt: PlayerDeathEvent) {
        val player = evt.entity
        if (player.hasMetadata("NPC")) return
        val killer = player.killer ?: return
        val killerName = PlayerProfile.getRawCache(killer.uniqueId).formattedNameWithRoman
        killerData[player.uniqueId] = KillerData(killerName, System.currentTimeMillis())
    }
}