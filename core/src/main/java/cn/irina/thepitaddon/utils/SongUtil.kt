package cn.irina.thepitaddon.utils

import net.mizukilab.pit.util.music.PositionSongPlayer
import net.mizukilab.pit.util.music.Song
import org.bukkit.Effect
import org.bukkit.entity.Player
import java.util.*

object SongUtil {
    @JvmStatic
    fun songPlay(target: Player, playerMap: MutableMap<UUID, PositionSongPlayer>, music: Song) {
        val songPlayer = playerMap[target.uniqueId]
        if (songPlayer == null) {
            val player = PositionSongPlayer(music)
            player.targetLocation = target.location
            player.autoDestroy = false
            player.isLoop = true
            player.isPlaying = true
            player.volume = 0.toByte()

            playerMap[target.uniqueId] = player
        } else {
            target.world.playEffect(target.location.clone().add(0.0, 3.0, 0.0), Effect.NOTE, 1)
        }
    }
}
