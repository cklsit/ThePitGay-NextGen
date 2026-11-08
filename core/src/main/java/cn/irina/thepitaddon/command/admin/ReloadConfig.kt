package cn.irina.thepitaddon.command.admin

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import org.bukkit.command.CommandSender
import java.io.File
import java.io.IOException

@Command(name = "thepitaddon", aliases = ["tpa"])
@Permission("pit.admin")
class ReloadConfig {
    @Execute(name = "reload")
    fun reloadConfig(@Context sender: CommandSender) {
        try {
            val main = Main.instance
            val configFile = File(main.dataFolder, "config.yml")
            val filterFile = File(main.dataFolder, "filter.yml")
            if (configFile.exists() && filterFile.exists()) {
                main.reloadConfig()
                main.getFilterManager().reloadFilterConfig()
                sender.sendMessage(CC.translate("${Main.instance.PREFIX} &a配置文件重载成功!"))
            } else {
                sender.sendMessage(CC.translate("${Main.instance.PREFIX} &c配置文件不存在!"))
            }
        } catch (exception: IOException) {
            sender.sendMessage(CC.translate("${Main.instance.PREFIX} &c重载配置文件时发生错误: ${exception.message}"))
            exception.printStackTrace()
        } catch (exception: Exception) {
            sender.sendMessage(CC.translate("${Main.instance.PREFIX} &c重载配置文件时发生未知错误: ${exception.message}"))
            exception.printStackTrace()
        }
    }

    companion object {
        private val PREFIX = Main.instance.PREFIX
    }
}