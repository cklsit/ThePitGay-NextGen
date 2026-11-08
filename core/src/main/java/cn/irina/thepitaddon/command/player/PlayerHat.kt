package cn.irina.thepitaddon.command.player

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Command(name = "hat")
@Permission("pit.hat")
class PlayerHat {
    @Execute
    fun hat(@Context player: Player) {
        val block = getTargetBlock(player)
        if (block == Material.AIR) {
            player.sendMessage(CC.translate("&c你没有看向任何方块!"))
            return
        }
        block?.let { replaceBlockToHelmetOrClear(player, it) }
    }

    private fun getTargetBlock(player: Player): Material? {
        val transparentBlocks = HashSet<Material>().apply {
            add(Material.AIR)
            add(Material.WATER)
            add(Material.LAVA)
        }
        val targetBlock = player.getTargetBlock(transparentBlocks, 4) ?: return null
        return targetBlock.type
    }

    private fun canWear(block: Material): Boolean {
        val denyBlocks = listOf(
            Material.SLIME_BLOCK,
            Material.ENCHANTMENT_TABLE,
            Material.BARRIER,
            Material.CHEST,
            Material.ENDER_CHEST
        )
        return block !in denyBlocks
    }

    private fun replaceBlockToHelmetOrClear(player: Player, block: Material) {
        val oldHelmet = player.inventory.helmet
        if (!canWear(block)) {
            player.sendMessage(CC.translate("&c你不能将 &f${block.name} &c带到头上!"))
            return
        }

        player.inventory.helmet = ItemStack(block)

        if (oldHelmet == null) {
            player.sendMessage(CC.translate("&a成功将头盔 &fAIR &a替换为 &f${block.name}"))
            return
        }
        if (isItem(oldHelmet.type)) {
            giveItemToPlayer(player, oldHelmet)
            player.sendMessage(CC.translate("&a已将 &f${oldHelmet.type.name} &a添加到背包中"))
            player.sendMessage(CC.translate("&a成功将头盔 &f${oldHelmet.type.name} &a替换为 &f${block.name}"))
        } else {
            player.sendMessage(CC.translate("&a已清除 &f${oldHelmet.type.name}"))
            player.sendMessage(CC.translate("&a成功将头盔 &f${oldHelmet.type.name} &a替换为 &f${block.name}"))
        }
    }


    private fun isItem(material: Material): Boolean {
        return !material.isBlock
    }

    private fun giveItemToPlayer(player: Player, item: ItemStack) {
        player.inventory.addItem(item)
    }
}