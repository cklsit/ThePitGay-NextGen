package cn.irina.thepitaddon.command.player

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import net.mizukilab.pit.menu.cdk.generate.CDKMenu
import net.mizukilab.pit.menu.perk.normal.choose.PerkChooseMenu
import net.mizukilab.pit.menu.prestige.PrestigeMainMenu
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/*
 * @Author Irina
 * @Date 2025/7/24 14:51
 */

@Command(name = "om")
class OpenMenu {
    private val pitApi = ThePit.getApi()
    private val prefix = Main.instance.PREFIX

    private val cd = ConcurrentHashMap<UUID, Cooldown>()
    private val cooldown = 15L

    @Execute
    fun handleCommand(
        @Context player: Player,
        @Flag("Shop", "S", "Prestige", "P", "Pre", "Perk", "PE", "CDK") menuName: String
    ) {
        if (player.hasPermission("pit.admin")) {
            handleOpenMenu(player, menuName)
            return
        }
        if ("CDK" == menuName && !player.hasPermission("pit.admin")) {
            player.sendMessage(CC.translate("&c你不能打开这个菜单."))
            return
        }
        val cd0: Cooldown = cd[player.uniqueId] ?: Cooldown(0L)
        if (!cd0.hasExpired()) {
            val lastCd = TimeUnit.MILLISECONDS.toSeconds(cd0.duration)
            player.sendMessage(CC.translate("&c冷却中, 请等待$lastCd&c秒..."))
            return
        }

        cd[player.uniqueId] = Cooldown(cooldown, TimeUnit.SECONDS)
        handleOpenMenu(player, menuName)
    }

    private fun handleOpenMenu(@Context player: Player, menu: String) {
        when (menu.uppercase()) {
            "SHOP", "S" -> pitApi.openMenu(player, "shop")
            "PRESTIGE", "P", "PRE" -> PrestigeMainMenu().openMenu(player)
            "PERK", "PE" -> PerkChooseMenu().openMenu(player)
            "CDK" -> CDKMenu().openMenu(player)
            else -> player.sendMessage(CC.translate("$prefix&c错误的菜单名称!"))
        }
    }
}