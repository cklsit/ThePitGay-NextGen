package net.mizukilab.pit

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.ThePit.setApi
import net.mizukilab.pit.impl.PitInternalImpl

object Loader {

    @JvmStatic
    fun start() {
        ThePit.getInstance().apply {
            setApi(PitInternalImpl)
        }
        // 启动已合并进主插件的 ThePitAddon-MythicLand 扩展
        runCatching { cn.irina.thepitaddon.Main().enable(ThePit.getInstance()) }
            .onFailure { ThePit.getInstance().logger.severe("ThePitAddon 扩展启动失败: ${it.message}"); it.printStackTrace() }
        PitHook.init()
    }
}
