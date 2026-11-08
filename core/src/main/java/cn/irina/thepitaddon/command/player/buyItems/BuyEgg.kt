package cn.irina.thepitaddon.command.player.buyItems

import cn.charlotte.pit.data.PlayerProfile
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


@Command(name = "buyEgg")
class BuyEgg {
    @Execute
    fun onDefault(@Context player: Player) {
        player.sendMessage(CC.translate("&cUsage: /buyEgg <effect>"))
    }


    @Execute(name = "speed")
    fun buySpeedEgg(@Context player: Player) {
        BuyUtil.giveItem(player, getSpeedEgg(), 1000)
    }

    @Execute(name = "resistance")
    fun buyResistanceEgg(@Context player: Player) {
        BuyUtil.giveItem(player, getResistanceEgg(), 1000)
    }

    @Execute(name = "health_boost")
    fun buyHealthBoostEgg(@Context player: Player) {
        BuyUtil.giveItem(player, getHealthBoostEgg(), 2000)
    }

    @Execute(name = "absorption")
    fun buyAbsorptionEgg(@Context player: Player) {
        BuyUtil.giveItem(player, getAbsorptionEgg(), 2000)
    }


    private fun getSpeedEgg(): ItemStack {
        val lore: MutableList<String> = ArrayList()

        lore.add("&7死亡后消失")
        lore.add("")
        lore.add("&7使用后获得 &b速度 III &f(00:08)")
        lore.add("&7(30秒冷却)")

        return ItemBuilder(Material.MONSTER_EGG)
            .name("&b速度蛋")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("egg")
            .durability(94)
            .build()
    }

    private fun getResistanceEgg(): ItemStack {
        val lore: MutableList<String> = ArrayList()

        lore.add("&7死亡后消失")
        lore.add("")
        lore.add("&7使用后获得 &3抗性提升 II &f(00:08)")
        lore.add("&7(30秒冷却)")

        return ItemBuilder(Material.MONSTER_EGG)
            .name("&3抗性蛋")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("egg")
            .durability(67)
            .build()
    }

    private fun getHealthBoostEgg(): ItemStack {
        val lore: MutableList<String> = ArrayList()

        lore.add("&7死亡后消失")
        lore.add("")
        lore.add("&7使用后获得 &c2❤ 额外生命值 &f(00:30)")
        lore.add("&7(90秒冷却) (不可叠加)")

        return ItemBuilder(Material.MONSTER_EGG)
            .name("&c生命提升蛋")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("egg")
            .durability(96)
            .build()
    }

    private fun getAbsorptionEgg(): ItemStack {
        val lore: MutableList<String> = ArrayList()

        lore.add("&7死亡后消失")
        lore.add("")
        lore.add("&7使用后获得 &68❤ 生命吸收 &f(00:30)")
        lore.add("&7(90秒冷却) (不可叠加)")

        return ItemBuilder(Material.MONSTER_EGG)
            .name("&6生命吸收蛋")
            .lore(lore)
            .removeOnJoin(false)
            .deathDrop(true)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("egg")
            .durability(62)
            .build()
    }
}

