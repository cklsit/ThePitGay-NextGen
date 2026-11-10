package net.mizukilab.pit.util

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.entity.Firework
import org.bukkit.inventory.meta.FireworkMeta

/**
 * 单次爆炸的烟花。
 *
 * 1.21 适配：原实现继承 NMS 的 EntityFireworks 并重写 tick 手动发包，
 * 现改用真实 Firework 实体，由服务端负责生命周期。
 */
class BoomFirework(private val location: Location) {

    fun spawn(color: Color?) {
        val world = location.world ?: return
        val firework = world.spawn(location, Firework::class.java)
        val meta: FireworkMeta = firework.fireworkMeta
        val effect = FireworkEffect.builder()
            .with(FireworkEffect.Type.BURST)
            .withColor(color ?: Color.WHITE)
            .build()
        meta.addEffect(effect)
        meta.power = 1
        firework.fireworkMeta = meta
    }
}
