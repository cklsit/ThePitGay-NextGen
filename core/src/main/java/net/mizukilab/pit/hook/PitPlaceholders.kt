package net.mizukilab.pit.hook

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.events.genesis.GenesisTeam
import net.mizukilab.pit.getPitProfile
import net.mizukilab.pit.listener.CombatListener
import net.mizukilab.pit.util.chat.CC
import org.bukkit.entity.Player

/**
 * 内置占位符解析，不依赖 PlaceholderAPI。
 *
 * <p>{@link PitPapiHook} 继承 PAPI 的 {@code PlaceholderExpansion}，未安装 PAPI 时类加载会失败；
 * 因此真正的解析逻辑放在这里，TAB、计分板等内部调用方直接用它，装了 PAPI 时再由
 * PitPapiHook 转发过来。</p>
 */
object PitPlaceholders {

    /** 返回值带 & 颜色代码；未知参数返回 null。 */
    @JvmStatic
    fun resolve(player: Player?, params: String): String? {
        val profile = player?.let {
            val loaded = it.getPitProfile()
            if (loaded.isLoaded) loaded else null
        }

        when (params) {
            "boost" -> {
                val boost = CombatListener.eventBoost
                if (boost <= 0) {
                    return "无"
                }
                return if (boost == boost.toLong().toDouble()) boost.toLong().toString() else boost.toString()
            }

            "next_map" -> {
                val map = ThePit.getInstance().configManager.getPitWorldConfigSpecific(
                    ThePit.getInstance().configManager.cursor + 1L
                )
                return if (map != null) map.worldName else "NO"
            }

            "cooldown_switchmap" -> return ThePit.getInstance().mapSelector.remainTime.toString()

            "level_tag_roman" -> return CC.translate(profile?.formattedLevelTagWithRoman ?: "&7[0]")
            "level_tag" -> return CC.translate(profile?.formattedLevelTag ?: "&7[0]")

            "genesis_tag" -> {
                if (ThePit.getInstance().pitConfig.isGenesisEnable) {
                    if (profile?.genesisData?.team == GenesisTeam.ANGEL) return "&b♆"
                    if (profile?.genesisData?.team == GenesisTeam.DEMON) return "&c♨"
                }
                return ""
            }

            "prestige" -> return profile?.prestige.toString()
            "coins_int" -> return profile?.coins?.toInt().toString()
            "coins" -> return profile?.coins?.toString() ?: "0.0"
            "exp" -> return profile?.experience?.toString() ?: "0.0"
            "bounty" -> return profile?.bounty?.toString() ?: "0"
        }

        return null
    }

    /** 替换文本中所有 %pit_xxx% 占位符。 */
    @JvmStatic
    fun apply(player: Player?, text: String): String {
        if (text == null || '%' !in text) {
            return text
        }
        var result = text
        var start = result.indexOf("%pit_")
        while (start >= 0) {
            val end = result.indexOf('%', start + 5)
            if (end < 0) break
            val key = result.substring(start + 5, end)
            val value = resolve(player, key) ?: ""
            result = result.substring(0, start) + value.replace("&", "§") + result.substring(end + 1)
            start = result.indexOf("%pit_", start + value.length)
        }
        return result
    }
}
