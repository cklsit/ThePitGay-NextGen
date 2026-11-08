package cn.irina.thepitaddon.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    @JvmStatic
    fun formatTotalSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    @JvmStatic
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return format.format(date)
    }
}
