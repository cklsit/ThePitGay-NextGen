package cn.irina.thepitaddon.command.player.buyItems

import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object BuyUtil {
    fun giveItem(player: Player, item: ItemStack, price: Int) {
        val profile = PlayerProfile.getRawCache(player.uniqueId) ?: return
        if (profile.coins < price) {
            player.sendMessage(CC.translate("&c你的硬币不足!"))
            return
        }
        profile.coins -= price
        player.playSound(player.location, Sound.NOTE_PLING, 1f, 1f)
        player.inventory.addItem(item)
        player.sendMessage(CC.translate("&a购买成功!"))
    }
}