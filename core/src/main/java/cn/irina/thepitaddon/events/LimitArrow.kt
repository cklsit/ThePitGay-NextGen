package cn.irina.thepitaddon.events

import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class LimitArrow : Listener {
    @EventHandler
    fun onPlayerShootsArrow(event: PlayerInteractEvent) {
        val player = event.player
        if (player.hasPermission("pit.shoot") &&
            event.action != null && event.action.name.contains("RIGHT_CLICK") &&
            player.itemInHand.type == Material.BOW
        ) {
            limitArrowCount(player)
        }
    }

    private fun limitArrowCount(player: Player) {
        val currentArrowCount = player.inventory.contents
            .filter { it != null && it.type == Material.ARROW }
            .sumOf { it.amount }

        if (currentArrowCount < 32) addArrows(player, 32 - currentArrowCount)
        if (currentArrowCount > 32) removeArrows(player, currentArrowCount - 32)
    }

    private fun addArrows(player: Player, amount: Int) {
        player.inventory.addItem(createArrowItem(amount).build())
    }

    private fun removeArrows(player: Player, amount: Int) {
        player.inventory.removeItem(createArrowItem(amount).build())
    }

    private fun createArrowItem(amount: Int): ItemBuilder {
        return ItemBuilder(Material.ARROW)
            .internalName("default_arrow")
            .defaultItem()
            .canDrop(false)
            .canSaveToEnderChest(false)
            .amount(amount)
    }
}