package cn.irina.thepitaddon.command.player

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 共享垃圾桶：/lj 打开，分页展示。
 *
 * 布局：第 1-5 行（0-44）为物品区，每页 45 格；
 * 最后一行为黑色玻璃板（名字空白），最左放“上一页”、最右放“下一页”。
 * 物品一旦投入不可再被玩家拾取，只能等待定时清理。
 */
@Command(name = "lj")
class TrashCommand : Listener {

    @Execute
    fun onDefault(@Context player: Player) {
        open(player, pageOf(player))
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.view.title != TITLE) return
        val player = event.whoClicked as? Player ?: return
        val top = event.view.topInventory
        val page = pageOf(player)
        event.isCancelled = true

        // 从玩家背包投入垃圾桶（shift 点击），投入后进入共享列表
        if (event.clickedInventory !== top) {
            if (event.isShiftClick) {
                val item = event.currentItem ?: return
                if (item.type.isAir) return
                items.add(item.clone())
                player.inventory.setItem(event.slot, null)
                refresh(player, page)
            }
            return
        }

        val slot = event.rawSlot
        if (slot !in 0 until CONTENT_SLOTS) {
            when (slot) {
                PREV_SLOT -> if (page > 0) open(player, page - 1)
                NEXT_SLOT -> if (page < totalPages() - 1) open(player, page + 1)
            }
            return
        }

        // 拾取：从共享列表取出并直接交给玩家（背包满则掉落在脚下）
        val index = page * CONTENT_SLOTS + slot
        val stored = items.getOrNull(index) ?: return
        items.removeAt(index)
        top.setItem(slot, null)
        val leftover = player.inventory.addItem(stored)
        leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.title != TITLE) return
        event.isCancelled = true
    }

    private fun refresh(player: Player, page: Int) {
        Bukkit.getScheduler().runTask(Main.instance.plugin, Runnable { if (player.isOnline) open(player, page) })
    }

    private fun open(player: Player, requestedPage: Int) {
        val pages = totalPages()
        val page = requestedPage.coerceIn(0, pages - 1)
        pagesByPlayer[player.uniqueId] = page

        val inventory = Bukkit.createInventory(player, 54, TITLE)
        val blankPane = ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name("&r")
            .build()

        for (slot in BOTTOM_ROW) {
            inventory.setItem(slot, blankPane)
        }

        inventory.setItem(PREV_SLOT, navButton("&e上一页", "&7第 &f${page + 1}&7 / &f$pages &7页", page > 0))
        inventory.setItem(NEXT_SLOT, navButton("&e下一页", "&7第 &f${page + 1}&7 / &f$pages &7页", page < pages - 1))

        val start = page * CONTENT_SLOTS
        for (index in 0 until CONTENT_SLOTS) {
            val stored = items.getOrNull(start + index) ?: break
            inventory.setItem(index, stored.clone())
        }

        player.openInventory(inventory)
    }

    private fun navButton(name: String, lore: String, enabled: Boolean): ItemStack =
        ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(name)
            .lore(if (enabled) lore else "$lore &c(已到边界)")
            .build()

    companion object {
        /** 标题要和 /bin 的“垃圾桶”区分开，避免它的监听器误判。 */
        private const val TITLE = "垃圾桶 (共享)"
        private const val CONTENT_SLOTS = 45
        private const val PREV_SLOT = 45
        private const val NEXT_SLOT = 53
        private val BOTTOM_ROW = 45..53

        /** 所有玩家共享的垃圾桶内容，按加入顺序分页展示。 */
        @JvmField
        val items: MutableList<ItemStack> = CopyOnWriteArrayList()

        private val pagesByPlayer = ConcurrentHashMap<UUID, Int>()

        @JvmStatic
        fun pageOf(player: Player): Int = pagesByPlayer[player.uniqueId] ?: 0

        @JvmStatic
        fun totalPages(): Int = maxOf(1, (items.size + CONTENT_SLOTS - 1) / CONTENT_SLOTS)

        @JvmStatic
        fun startClearTask() {
            val minutes = Main.instance.cfg.getInt("Trash.ClearInterval-Minutes", 5).coerceAtLeast(1)
            val message = Main.instance.cfg.getString("Trash.ClearMessage") ?: "&7垃圾桶 >> &c垃圾桶里的物品已被清空"
            val intervalTicks = minutes * 60L * 20L
            Bukkit.getScheduler().runTaskTimer(
                Main.instance.plugin,
                Runnable {
                    items.clear()
                    pagesByPlayer.clear()
                    Bukkit.broadcastMessage(CC.translate(message))
                },
                intervalTicks,
                intervalTicks
            )
        }
    }
}
