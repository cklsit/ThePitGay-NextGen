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
        PitHook.init()
        // 启动已合并进主插件的 ThePitAddon-MythicLand 扩展
        cn.irina.thepitaddon.Main().enable(ThePit.getInstance())
    }
}
