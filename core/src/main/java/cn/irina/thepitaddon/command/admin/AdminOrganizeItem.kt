package cn.irina.thepitaddon.command.admin

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.manager.PitManager
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.entity.Player
import java.util.*

@Permission("pit.admin")
@Command(name = "organize", aliases = ["org", "organ"])
class AdminOrganizeItem {

    private val defUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Execute
    fun onCommand(
        @Context player: Player,
        @Arg level: Int,
        @Arg live: Int,
        @Arg maxLives: Int
    ) {
        val itemInHand = player.inventory.itemInHand
        when {
            itemInHand == null -> {
                player.sendMessage(CC.translate("&c请手持物品!"))
                return
            }

            live < 0 || maxLives < 0 -> {
                player.sendMessage(CC.translate("&c请输入正确的物品生命值!"))
                return
            }

            level > 3 || level < 1 -> {
                player.sendMessage(CC.translate("&c请输入正确的物品等级!"))
                return
            }

            else -> {
                val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(itemInHand)
                if (pitItem == null) {
                    player.sendMessage(CC.translate("&c该物品不需要刷新!"))
                    return
                }

                val pitItemToStack = pitItem.toItemStack()
                ItemBuilder(pitItemToStack)
                    .changeNbt("maxLive", maxLives)
                    .changeNbt("live", live)
                    .changeNbt("tier", level)
                    .uuid(defUUID)
                    .buildWithUnbreakable()
                player.inventory.itemInHand = pitItemToStack
                player.sendMessage(CC.translate("&a物品整理完成!"))
                PitManager.flushPlayerItem(player)
            }
        }
    }
}