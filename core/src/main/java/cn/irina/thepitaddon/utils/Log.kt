package cn.irina.thepitaddon.utils

import net.mizukilab.pit.util.chat.CC
import cn.irina.thepitaddon.Main
import org.bukkit.Bukkit

object Log {
    private val PREFIX = Main.instance.PREFIX

    @JvmStatic
    fun send(message: String) {
        Bukkit.getConsoleSender().sendMessage(CC.translate(PREFIX + message))
    }
}
