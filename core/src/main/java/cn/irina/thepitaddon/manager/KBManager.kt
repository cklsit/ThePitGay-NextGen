package cn.irina.thepitaddon.manager

import cn.irina.thepitaddon.Main
import net.mizukilab.pit.util.PlayerUtil
import org.bukkit.entity.NPC
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

class KBManager : Listener {

    /*
    * @Author ShanguanLinG
    * @Date 2025/9/13 23:52
    */


    private val kbAPI = Main.instance.kbmAPI

    private fun hasRegEnchant(leggings: ItemStack): Boolean {
        val hasRegEnchant = leggings.let {
            PitManager.hasPitEnchant(
                it,
                "regularity"
            ) ?: false
        }
        return hasRegEnchant
    }

    private fun hasThinkOfThePeopleEnchant(leggings: ItemStack): Boolean {
        val think_of_the_people = leggings.let {
            PitManager.hasPitEnchant(
                it,
                "think_of_the_people"
            ) ?: false
        }
        return think_of_the_people
    }


    private fun hasGrimReaperEnchant(leggings: ItemStack): Boolean {
        return leggings.let {
            PitManager.hasPitEnchant(
                it,
                "grim_reaper_enchant"
            )
        } ?: false
    }

    private fun isDarkLeggings(leggings: ItemStack): Boolean {
        return leggings.let {
            PitManager.hasPitEnchant(
                it,
                "somber_enchant"
            )
        } ?: false
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.entity is NPC) return
        if (event.damager !is Player || event.entity !is Player) return
        val damager = event.damager as Player
        val other: Player = event.entity as Player
        if (PitManager.isNPC(other)) return
        kbAPI?.let { api ->
            val inventory = damager.inventory
            val otherLeggings = other.inventory.leggings
            val leggings = inventory.leggings
            if (inventory == null || leggings == null || otherLeggings == null) {
                api.setKBFile(other, "default")
                return
            }
            when {
                hasRegEnchant(leggings) -> {
                    if (hasThinkOfThePeopleEnchant(otherLeggings)
                        || isDarkLeggings(otherLeggings)
                        || PlayerUtil.isVenom(other)
                    ) {
                        api.setKBFile(other, "default")
                        return
                    }
                    api.setKBFile(other, "regularity")
                    return
                }

                hasGrimReaperEnchant(leggings) -> {
                    api.setKBFile(other, "grim_reaper_enchant")
                    return
                }

                else -> {
                    api.setKBFile(other, "default")
                    return
                }
            }
        }

    }
}