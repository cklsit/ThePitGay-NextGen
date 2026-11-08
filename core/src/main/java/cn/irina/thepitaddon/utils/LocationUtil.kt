package cn.irina.thepitaddon.utils

import net.minecraft.server.v1_8_R3.DamageSource.arrow
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player


object LocationUtil {
    fun getClosestPlayer(size: Int, myself: Entity): Player? {
        val nearbyEntities = myself.getNearbyEntities(size.toDouble(), size.toDouble(), size.toDouble())
        var target: Player? = null
        var distance = Double.MAX_VALUE

        for (entity in nearbyEntities) {
            val closestDistance = entity.location.distance(myself.location)
            if (closestDistance < distance) {
                distance = closestDistance
                if (entity is Player) target = entity
            }
        }

        return target
    }

    fun isInWater(myself: Player): Boolean {
        val block: Block = myself.location.block
        val type: Material = block.type
        return type == Material.WATER || type == Material.STATIONARY_WATER
    }

    fun getNearestPlayer(center: Entity, radius: Double): Player? {
        return center.getNearbyEntities(radius, radius, radius)
            .filterIsInstance<Player>()
            .filter { it != center }
            .minByOrNull { it.location.distanceSquared(center.location) }
    }
}
