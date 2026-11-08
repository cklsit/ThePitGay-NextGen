package cn.irina.thepitaddon.command.player.buyItems

import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Command(name = "buyIronHelmet")
class BuyIronHelmet {
    @Execute
    fun buyIronHelmet(@Context player: Player) {
        BuyUtil.giveItem(player, ironHelmet, 100)
    }

    private val ironHelmet: ItemStack by lazy {
        val lore: MutableList<String> = ArrayList()
        lore.add("&6从时空商人处获得")

        ItemBuilder(Material.IRON_HELMET)
            .name("&f铁头盔")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("shopItem")
            .buildWithUnbreakable()
    }
}
