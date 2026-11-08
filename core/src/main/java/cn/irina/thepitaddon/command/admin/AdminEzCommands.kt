package cn.irina.thepitaddon.command.admin

import cn.charlotte.pit.data.PlayerProfile
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.join.Join
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.SpecialUtil
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


@Command(name = "heal")
@Permission("pit.admin")
class AdminHealSelf {
    @Execute
    fun heal(@Context player: Player) {
        player.health = player.maxHealth
        player.foodLevel = 20
        player.sendMessage(CC.translate("&aSUCCESS."))
    }
}

@Command(name = "clearBounty")
@Permission("pit.admin")
class AdminClearBounty {
    @Execute
    fun clearBounty(@Context player: Player) {
        val profile = PlayerProfile.getRawCache(player.uniqueId)
        profile.bounty = 0
        player.sendMessage(CC.translate("&aSUCCESS."))
    }
}

@Command(name = "setHitDelay")
@Permission("pit.admin")
class SetHitDelay {
    @Execute
    fun onCommand(
        @Arg player: Player,
        @Arg delay: Int,
        @Arg maxDelay: Int
    ) {
        player.noDamageTicks = delay
        player.maximumNoDamageTicks = maxDelay
        player.sendMessage(CC.translate("&aSUCCESS."))
    }
}

@Command(name = "private")
@Permission("pit.private")
class Private {
    @Execute
    fun onCommand(@Context player: Player) {
        if (SpecialUtil.isPrivate(player)) {
            SpecialUtil.removePlayer(player)
            return
        }
        SpecialUtil.addPlayer(player)
    }
}

@Command(name = "fixWipe")
@Permission("pit.admin")
class FixWipe {
    @Execute
    fun onCommand(@Context sender: Player, @Arg player: Player) {
        if (!player.isOnline) {
            sender.sendMessage(CC.translate("&c目标玩家并不在线"))
            return
        }

        val profile = PlayerProfile.getRawCache(player.uniqueId)
        if (profile.wipedData != null) {
            profile.wipedData.isKnow = true
            player.sendMessage(CC.translate("&aSuccess."))
        }
    }
}

@Command(name = "worldTp")
@Permission("pit.admin")
class WorldTp {
    @Execute
    fun onCommand(
        @Context sender: CommandSender,
        @Arg player: Player,
        @Arg worldName: String
    ) {
        val targetWorld: World? = Bukkit.getWorld(worldName)
        if (targetWorld == null) {
            player.sendMessage(CC.translate("&c目标世界不存在!"))
            return
        }
        player.teleport(targetWorld.spawnLocation)
    }
}

@Command(name = "broadcast")
@Permission("pit.admin")
class Broadcast {
    @Execute
    fun onCommand(
        @Context sender: CommandSender,
        @Join(separator = " ") message: String
    ) {
        Bukkit.broadcastMessage(CC.translate(message))
    }
}

