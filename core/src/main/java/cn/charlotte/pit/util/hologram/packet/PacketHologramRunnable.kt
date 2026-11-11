package cn.charlotte.pit.util.hologram.packet

import cn.hutool.core.collection.ConcurrentHashSet
import org.bukkit.scheduler.BukkitRunnable

class PacketHologramRunnable : BukkitRunnable() {
    override fun run() {
        holograms.removeIf {
            !it.spawned
        }
        holograms.forEach {
            it.update()
        }
    }

    companion object {
        val holograms: MutableSet<PacketHologram> = ConcurrentHashSet()

        @JvmStatic
        fun deSpawnAll() {
            holograms.forEach {
                // entity 只有在第一次 update/spawn 之后才存在；未生成过的全息这里会是 null，
                // 直接 remove() 会让插件 disable 阶段抛 NPE。
                it.hologram.entity?.remove()
            }
            holograms.clear()
        }
    }
}