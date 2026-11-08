package cn.irina.thepitaddon.command.player.buyItems

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 重构商店购买的逻辑
 * @author ShanguanLinG
 * @since 2025/5/19
 */

@Command(name = "buyExDiamondItem")
class BuyExDiamondItem {
    private val prefix = Main.instance.PREFIX

    @Execute
    fun onCommand(@Context player: Player, @Arg str: String) {
        when (str.uppercase()) {
            "SWORD" -> BuyUtil.giveItem(player, exDiamondSword, 650)

            else -> player.sendMessage(CC.translate("$prefix&c/buyExDiamondItem <ItemName>"))
        }
    }

    private val exDiamondSword: ItemStack by lazy {
        val lore: MutableList<String> = ArrayList()

        lore.add("&7从时空商人处获得")
        lore.add("")
        lore.add("&7死亡后消失")
        lore.add("")
        lore.add("&9攻击伤害 +8.25")
        lore.add("&9无法破坏")

        ItemBuilder(Material.DIAMOND_SWORD)
            .shiny()
            .name("&c强化钻石剑")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("shopItem")
            .itemDamage(7.0)
            .enchantment(Enchantment.DAMAGE_ALL, 1)
            .buildWithUnbreakable()
    }
}

