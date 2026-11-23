package net.mizukilab.pit.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * PlaceholderAPI 扩展，只做转发；真正的解析在 {@link PitPlaceholders}，
 * 未安装 PAPI 时不会被加载。
 */
object PitPapiHook : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "pit"
    }

    override fun getAuthor(): String {
        return "ThePitUltimate"
    }

    override fun getVersion(): String {
        return "1.0"
    }

    override fun onPlaceholderRequest(p: Player, params: String): String? {
        return PitPlaceholders.resolve(p, params)
    }
}
