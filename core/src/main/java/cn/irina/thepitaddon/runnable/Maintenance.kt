package cn.irina.thepitaddon.runnable

import org.bukkit.event.Listener

class Maintenance : Listener {
    companion object {
        @JvmField
        var isMaintenanceMode = false

        @JvmStatic
        fun isInMaintenanceMode(): Boolean = isMaintenanceMode

        @JvmStatic
        fun setMaintenanceMode(mode: Boolean) {
            isMaintenanceMode = mode
        }
    }
}