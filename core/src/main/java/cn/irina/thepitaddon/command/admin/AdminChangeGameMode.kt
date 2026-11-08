package cn.irina.thepitaddon.command.admin

import net.mizukilab.pit.util.chat.CC
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.GameMode
import org.bukkit.entity.Player

@Command(name = "gm")
@Permission("pit.admin")
class AdminChangeGameMode {
    @Execute
    fun changeMySelfGameMode(@Context player: Player, @Arg gameModeInt: Int) {
        if (switchGameMode(gameModeInt) != null) {
            player.gameMode = switchGameMode(gameModeInt)
            player.sendMessage(CC.translate(PREFIX + " &7已将您的游戏模式设置为 &e" + switchGameModeString(gameModeInt)))
        } else {
            player.sendMessage(CC.translate("$PREFIX&c错误的游戏模式!"))
        }
    }

    companion object {
        private val PREFIX = Main.instance.PREFIX

        private fun switchGameMode(gameModeInt: Int): GameMode? {
            when (gameModeInt) {
                3 -> return GameMode.SPECTATOR
                2 -> return GameMode.ADVENTURE
                1 -> return GameMode.CREATIVE
                0 -> return GameMode.SURVIVAL
            }
            return null
        }

        private fun switchGameModeString(gameModeInt: Int): String {
            when (gameModeInt) {
                0 -> return "生存"
                1 -> return "创造"
                2 -> return "冒险"
                3 -> return "旁观"
            }
            return "NULL"
        }
    }
}
