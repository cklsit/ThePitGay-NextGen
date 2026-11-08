package cn.irina.thepitaddon.command.player

import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import net.mizukilab.pit.util.item.ItemUtil
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

@Command(name = "bin")
class PlayerOpenTrash : Listener {
    @Execute
    fun openTrash(@Context player: Player) {
        createInventory(player)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.view.title != TRASH_NAME) return

        val player = event.whoClicked as Player
        val clickItem = event.currentItem

        if (clickItem == null || clickItem.type == Material.AIR) return

        if (clickItem.type == Material.SKULL_ITEM) {
            event.isCancelled = true
            return
        }

        val internalName = ItemUtil.getInternalName(clickItem) ?: return

        when (internalName.lowercase(Locale.getDefault())) {
            "barrier" -> event.isCancelled = true
            else -> event.isCancelled = false
        }
    }

    private fun createInventory(player: Player) {
        val inventory = Bukkit.createInventory(player, 54, TRASH_NAME)

        val glassPane = ItemBuilder(ItemStack(Material.STAINED_GLASS_PANE)).internalName("barrier").name("&r").build()
        glassPane.durability = 15.toShort()


        val slots: List<Int> = ArrayList(
            listOf(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53
            )
        )

        for (slot in slots) {
            if (slot == 4) {
                inventory.setItem(
                    slot, ItemBuilder(ItemStack(Material.SKULL_ITEM, 1, 3.toShort()))
                        .lore(CC.translate("&c丢弃无用的物品"))
                        .name("&r")
                        .setSkullOwner(player.name)
                        .build()
                )
                continue
            }
            inventory.setItem(slot, glassPane)
        }

        player.openInventory(inventory)
    }

    companion object {
        private const val TRASH_NAME = "垃圾桶"
    }
}
