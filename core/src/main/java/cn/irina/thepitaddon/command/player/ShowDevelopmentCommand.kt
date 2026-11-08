package cn.irina.thepitaddon.command.player

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import org.bukkit.command.CommandSender

@Command(name = "thepitaddon")
class ShowDevelopmentCommand {

    @Execute
    fun development(@Context sender: CommandSender) {
        sender.sendMessage(CC.translate("&7ThePitAddon For &cThePitUltimate"))
        sender.sendMessage(CC.translate("&7Powered By &e_Ir1na_&7, &eShanguanLinG"))
    }
}
