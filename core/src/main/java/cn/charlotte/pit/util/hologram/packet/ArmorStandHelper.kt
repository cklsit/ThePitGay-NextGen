package cn.charlotte.pit.util.hologram.packet

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity

/**
 * 2024/5/16<br></br>
 * ThePitPlus<br></br>
 *
 * @author huanmeng_qwq
 *
 * 1.21 迁移：原先直接构造 NMS EntityArmorStand（v1_8_R3）以规避 Bukkit 的实体注册，
 * 现在统一走 Bukkit API —— 调用方负责在合适的世界/区块内创建并自行清理。
 */
class ArmorStandHelper {
    companion object {
        @JvmStatic
        fun applyLocation(location: Location, armorStand: PacketArmorStand) {
            armorStand.move(location, true)
        }

        /**
         * 1.8 时代通过 NMS 构造「游离」盔甲架（不进实体系统）；1.21 无对应入口，
         * 改为在世界中正常生成一个不可见、无碰撞的盔甲架。
         */
        @JvmStatic
        @JvmOverloads
        fun memoryEntity(location: Location): ArmorStand {
            val world = location.world ?: throw IllegalStateException("世界未加载: $location")
            val stand = world.spawn(location, ArmorStand::class.java)
            stand.isVisible = false
            stand.isMarker = true
            stand.isSmall = true
            stand.isInvulnerable = true
            stand.setGravity(false)
            stand.isCollidable = false
            stand.setCanPickupItems(false)
            return stand
        }

        @JvmStatic
        fun setEntityLocation(entity: Entity, to: Location) {
            entity.teleport(to)
        }
    }
}
