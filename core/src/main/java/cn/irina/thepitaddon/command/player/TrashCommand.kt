package cn.irina.thepitaddon.command.player

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * 垃圾桶：/lj 打开一个共享的大箱子界面，所有人可查看并拾取其中的物品。
 * 默认每 5 分钟清空一次并广播提示。
 */
@Command(name = "lj")
class TrashCommand {

    @Execute
    fun onDefault(@Context player: Player) {
        player.openInventory(trashInventory)
    }

    companion object {
        // 共享垃圾桶（所有玩家看到同一份内容）
        @JvmField
        val trashInventory: Inventory = Bukkit.createInventory(null, 54, CC.translate("垃圾桶"))

        fun startClearTask() {
            // 注意：getInt 对缺失 key 返回 0（非 null），必须用带默认值的重载
            val minutes = Main.instance.cfg.getInt("Trash.ClearInterval-Minutes", 5).coerceAtLeast(1)
            val message = Main.instance.cfg.getString("Trash.ClearMessage") ?: "&7垃圾桶 >> &c垃圾桶里的物品已被清空"
            val intervalTicks = minutes * 60L * 20L
            Bukkit.getScheduler().runTaskTimer(
                Main.instance.plugin,
                Runnable {
                    trashInventory.clear()
                    Bukkit.broadcastMessage(CC.translate(message))
                },
                intervalTicks,
                intervalTicks
            )
        }
    }
}