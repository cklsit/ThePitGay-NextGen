package cn.irina.thepitaddon.command.player

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.scheduler.BukkitTask

@Command(name = "ping")
class Ping {
    private val pingCheckTasks = mutableMapOf<java.util.UUID, BukkitTask>()

    @Execute
    fun ping(@Context player: Player) {
        val (ping, pingColor) = getPingAndPingColor(player)
        player.sendMessage(CC.translate("&fYour ping is $pingColor$ping ms"))
    }

    @Execute(name = "all")
    fun pingAll(@Context player: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            val (ping, pingColor) = getPingAndPingColor(onlinePlayer)
            player.sendMessage(CC.translate("&f${onlinePlayer.name}'s ping is $pingColor$ping ms"))
        }
    }

    @Execute(name = "player")
    fun pingPlayer(@Context player: Player, @Arg target: Player) {
        if (target.isOnline.not()) {
            player.sendMessage(CC.translate("&c${target.name} not online!"))
            return
        }
        val (ping, pingColor) = getPingAndPingColor(target)
        player.sendMessage(CC.translate("&f${target.name}'s ping is $pingColor$ping ms"))
    }

    @Execute(name = "check")
    fun pingCheck(@Context player: Player) {
        val uuid = player.uniqueId
        if (pingCheckTasks.containsKey(uuid)) {
            pingCheckTasks[uuid]?.cancel()
            pingCheckTasks.remove(uuid)
            player.sendMessage(CC.translate("&aStopped checking ping."))
        } else {
            val task = Bukkit.getScheduler().runTaskTimer(Main.instance, {
                val (ping, pingColor) = getPingAndPingColor(player)
                player.sendMessage(CC.translate("&fYour ping is $pingColor$ping ms"))
            }, 0L, 10L)
            pingCheckTasks[uuid] = task
        }
    }

    @Execute(name = "checkPlayer")
    fun pingCheckPlayer(@Context player: Player, @Arg target: Player) {
        val uuid = player.uniqueId
        if (pingCheckTasks.containsKey(uuid)) {
            pingCheckTasks[uuid]?.cancel()
            pingCheckTasks.remove(uuid)
            player.sendMessage(CC.translate("&aStopped checking ping."))
        } else {
            val task = Bukkit.getScheduler().runTaskTimer(Main.instance, {
                if (!target.isOnline) {
                    pingCheckTasks[uuid]?.cancel()
                    pingCheckTasks.remove(uuid)
                    player.sendMessage(CC.translate("&c${target.name} not online!"))
                    return@runTaskTimer
                }
                val (ping, pingColor) = getPingAndPingColor(target)
                player.sendMessage(CC.translate("&f${target.name}'s ping is $pingColor$ping ms"))
            }, 0L, 10L)
            pingCheckTasks[uuid] = task
        }
    }

    private fun getPingAndPingColor(player: Player): Pair<Int, String> {
        val ping = (player as CraftPlayer).handle.ping
        val pingColor = when {
            ping < 100 -> "&a"
            ping < 200 -> "&e"
            ping < 300 -> "&6"
            ping < 400 -> "&c"
            else -> "&4"
        }
        return Pair(ping, pingColor)
    }

    @EventHandler
    fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        pingCheckTasks[uuid]?.cancel()
        pingCheckTasks.remove(uuid)
    }
}