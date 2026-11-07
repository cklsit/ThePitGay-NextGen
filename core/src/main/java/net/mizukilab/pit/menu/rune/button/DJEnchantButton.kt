package cn.charlotte.pit.menu.rune.button

import cn.charlotte.pit.data.sub.EnchantmentRecord
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import net.mizukilab.pit.util.item.ItemUtil
import net.mizukilab.pit.util.menu.Button
import net.mizukilab.pit.util.Utils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * @author Araykal
 * @since 2025/1/16
 */
class DJEnchantButton(
    private val enchantment: AbstractEnchantment,
    private val itemStack: ItemStack,
    private val index: Int
) : Button() {

    override fun getButtonItem(player: Player): ItemStack {
        val playerItem = player.itemInHand

        if (isNullOrAir(playerItem) || ItemUtil.getInternalName(playerItem) == null) {
            return ItemBuilder(Material.AIR).build()
        }

        val lore = enchantment.getUsefulnessLore(1).split("/s").toTypedArray()
        return ItemBuilder(Material.JUKEBOX)
            .name("&9" + enchantment.getEnchantName())
            .lore(*lore)
            .shiny()
            .build()
    }

    override fun clicked(
        player: Player,
        slot: Int,
        clickType: ClickType,
        hotbarButton: Int,
        currentItem: ItemStack
    ) {
        if (ItemUtil.getInternalName(itemStack) == "mythic_leggings") {
            // def uuid 未鉴定的神话之甲也能谱写，改用按内部名构造
            val leggingsItem = Utils.getMythicItem0(itemStack)
            if (leggingsItem == null) {
                // 兜底：识别失败时绝不 setItem(index, null)，否则会把玩家装备清掉
                player.sendMessage(CC.translate("&c无法识别该神话之甲，已取消操作"))
                player.closeInventory()
                return
            }
            leggingsItem.enchantments[enchantment] = 1
            leggingsItem.enchantmentRecords += EnchantmentRecord(
                player.name,
                "MusicRune",
                System.currentTimeMillis()
            )
            player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 99f)
            player.sendMessage(CC.translate("&6&l铸造！ &7成功为你的神话之裤增加了${enchantment.enchantName}附魔"))

            player.inventory.setItem(index, leggingsItem.toItemStack())
            updatePlayerInventory(player)

            player.closeInventory()
        }
    }

    private fun isNullOrAir(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR
    }

    private fun updatePlayerInventory(player: Player) {
        val itemInHand = player.inventory.itemInHand

        if (itemInHand.amount == 1) {
            player.inventory.setItem(player.inventory.heldItemSlot, null)
        } else {
            itemInHand.amount -= 1
            player.inventory.setItem(player.inventory.heldItemSlot, itemInHand)
        }
    }
}
