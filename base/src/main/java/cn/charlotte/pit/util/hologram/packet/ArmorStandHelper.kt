package cn.charlotte.pit.util.hologram.packet

import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * 2024/5/16<br></br>
 * ThePitPlus<br></br>
 *
 * 1.21 适配：移除 NMS 幽灵实体，所有位置操作走 Bukkit 实体 API。
 *
 * @author huanmeng_qwq
 */
object ArmorStandHelper {

    @JvmStatic
    fun applyLocation(location: Location, armorStand: PacketArmorStand) {
        armorStand.move(location, true)
    }

    @JvmStatic
    fun setEntityLocation(entity: Entity, to: Location) {
        entity.teleport(to)
    }
}
