package cn.charlotte.pit.menu.rune

import cn.charlotte.pit.menu.rune.button.ItemRuneButton
import net.mizukilab.pit.music
import net.mizukilab.pit.util.item.ItemUtil
import net.mizukilab.pit.util.Utils.getMythicItem
import net.mizukilab.pit.util.menu.Button
import net.mizukilab.pit.util.menu.Menu
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * @author Araykal
 * @since 2025/1/16
 */
class MusicalRuneMenu : Menu() {


    override fun getTitle(player: Player): String {
        return "乐章谱写"
    }

    override fun getButtons(player: Player): MutableMap<Int, Button> {
        val map = HashMap<Int, Button>()
        var index = 0

        for ((slot, itemStack) in player.inventory.withIndex()) {
            if (itemStack == null || itemStack.type == Material.AIR) {
                continue
            }
            val mythic = getMythicItem(itemStack)
            Bukkit.getLogger().info("[RuneDebug] slot=" + slot
                    + " type=" + itemStack.type
                    + " internal=" + ItemUtil.getInternalName(itemStack)
                    + " mythic=" + (mythic != null)
                    + " hasMusic=" + music.any { it.isItemHasEnchant(itemStack) })
            if (isItemValid(itemStack)) {
                Bukkit.getLogger().info("[RuneDebug] 命中有效护甲 slot=" + slot)
                map[index++] = ItemRuneButton(itemStack, slot)
            }
        }
        return map
    }

    private fun isItemValid(itemStack: ItemStack?): Boolean {
        // 神话之甲可能带默认 uuid（未鉴定），getMythicItem 会因 uuid hash 为 -1 返回 null，
        // 故直接按 PDC 内部名判断，避免依赖 uuid。
        return itemStack?.type == Material.LEATHER_LEGGINGS &&
                "mythic_leggings" == ItemUtil.getInternalName(itemStack) &&
                !music.any { it.isItemHasEnchant(itemStack) }
    }

    override fun onClickEvent(event: InventoryClickEvent?) {
        event?.isCancelled = true
    }
}

