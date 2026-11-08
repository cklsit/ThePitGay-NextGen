package cn.irina.thepitaddon.data

import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import kotlin.experimental.and
import kotlin.experimental.or

/*
 * @Author Irina
 * @Date 2025/10/4 15:32
 */

data class PhantasmState(
    var task: BukkitTask? = null,
    var level: Int = 0,
    var startTime: Long = 0,
    val defaultSpeed: Float = 0.2F,
    var cooldown: Cooldown = Cooldown(0)
) {
    private var flags: Byte = 0

    var isActive: Boolean
        get() = (flags and 0x01) != 0.toByte()
        set(value) { flags = if (value) (flags or 0x01) else (flags and 0xFE.toByte()) }

    var isPerfect: Boolean
        get() = (flags and 0x02) != 0.toByte()
        set(value) { flags = if (value) (flags or 0x02) else (flags and 0xFD.toByte()) }

    fun cleanUp(player: Player? = null) {
        task?.cancel()
        task = null
        flags = 0 // 快速重置所有状态
        player?.walkSpeed = defaultSpeed
    }
}
