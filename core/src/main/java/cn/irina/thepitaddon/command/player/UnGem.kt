package cn.irina.thepitaddon.command.player

import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.manager.PitManager
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import net.mizukilab.pit.util.item.ItemUtil
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/*
 * @Author ShanguanLinG
 * @Date 2025/8/15
 */

@Command(name = "unGem")
class UnGem {
    val prefix = Main.instance.PREFIX
    private val reverseGemInternalName = "reverse_totally_legit_gem"
    private val reverseGlobalGemInternalName = "reverse_global_attention_gem"
    private val reverseUniversalGemInternalName = "reverse_gem"

    @Execute
    fun onCommand(@Context sender: CommandSender, @Arg gemTypeName: String, @Arg player: Player) {
        val item = player.inventory.itemInHand ?: return
        if (item.type == Material.AIR) {
            sender.sendMessage(CC.translate("$prefix&c请手持物品!"))
            return
        }

        val itObj = ItemBuilder(item)
        when (gemTypeName) {
            "Gem" -> {
                if (!isBoostedByGem(item)) {
                    sender.sendMessage(CC.translate("$prefix&c这件物品上并没有使用过 &a遵纪守法的宝石!"))
                    return
                }
                if (!PitManager.hasEnoughInternalItem(player, reverseGemInternalName, 1)) {
                    sender.sendMessage(CC.translate("$prefix&c你没有足够的 &5逆向宝石"))
                    return
                }

                itObj.changeNbt("boostedByGem", 0)
                takeInternalGem(player, reverseGemInternalName)

                player.sendMessage(CC.translate("$prefix&a成功移除了 &a遵纪守法的宝石 &7点缀."))
            }

            "GlobalGem" -> {
                if (!isBoostedByGlobalGem(item)) {
                    sender.sendMessage(CC.translate("$prefix&c这件物品上并没有使用过 &b举世瞩目的宝石!"))
                    return
                }
                if (!PitManager.hasEnoughInternalItem(player, reverseGlobalGemInternalName, 1)) {
                    sender.sendMessage(CC.translate("$prefix&c你没有足够的 &5逆向宝石"))
                    return
                }

                itObj.changeNbt("boostedByGlobalGem", 0).build()
                takeInternalGem(player, reverseGlobalGemInternalName)

                player.sendMessage(CC.translate("$prefix&a成功移除了 &b举世瞩目的宝石 &7点缀."))
            }

            "All" -> {
                if (!isBoostedByGem(item) && !isBoostedByGlobalGem(item)) {
                    sender.sendMessage(CC.translate("$prefix&c这件物品上并没有使用过任何宝石! "))
                    return
                }
                if (!PitManager.hasEnoughInternalItem(player, reverseUniversalGemInternalName, 1)) {
                    sender.sendMessage(CC.translate("$prefix&c你没有足够的 &5逆向宝石"))
                    return
                }

                itObj.changeNbt("boostedByGem", 0).changeNbt("boostedByGlobalGem", 0).build()
                takeInternalGem(player, reverseUniversalGemInternalName)

                player.sendMessage(CC.translate("$prefix&a成功移除了所有宝石点缀. 请将神话物品放入附魔台刷新!"))
            }
        }

        player.inventory.itemInHand = itObj.build()
        PitManager.flushPlayerItem(player)
    }

    private fun takeInternalGem(player: Player, internalName: String) {
        PitManager.takeInternalItem(player, internalName, 1)
    }

    private fun isBoostedByGem(item: ItemStack?): Boolean {
        val itemStringData = ItemUtil.getItemBoolData(item, "boostedByGem")
        return itemStringData
    }

    private fun isBoostedByGlobalGem(item: ItemStack?): Boolean {
        val itemStringData = ItemUtil.getItemBoolData(item, "boostedByGlobalGem")
        return itemStringData
    }
}
