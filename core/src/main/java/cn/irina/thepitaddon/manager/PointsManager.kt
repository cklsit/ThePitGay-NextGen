package cn.irina.thepitaddon.manager

import cn.irina.thepitaddon.Main
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.entity.Player

/*
 * @Author Irina
 * @Date 2025/6/18 17:44
 */

object PointsManager {
    val pointsAPI: PlayerPointsAPI?
        get() = Main.instance.pointsAPI

    fun getPoints(player: Player): Int {
        return pointsAPI?.look(player.uniqueId) ?: 0
    }

    fun addPoints(player: Player, amount: Int) {
        pointsAPI?.give(player.uniqueId, amount)
    }

    fun takePoints(player: Player, amount: Int) {
        pointsAPI?.take(player.uniqueId, amount)
    }

    fun setPoints(player: Player, amount: Int) {
        pointsAPI?.set(player.uniqueId, amount)
    }
}