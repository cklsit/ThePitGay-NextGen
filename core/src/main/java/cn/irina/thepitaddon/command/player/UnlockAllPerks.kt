package cn.irina.thepitaddon.command.player

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.charlotte.pit.data.sub.PerkData
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/*
 * @Author Irina
 * @Date 2025/6/23 18:01
 */

@Command(name = "unlockAllPerks")
@Permission("pit.unlock")
class UnlockAllPerks {
    val prefix = Main.instance.PREFIX

    @Execute
    fun onCommand(@Context player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
            val pp = PlayerProfile.getRawCache(player.uniqueId)

            if (pp == null) {
                player.sendMessage(CC.translate("$prefix&c你的玩家数据并不存在...认真的吗?"))
                return@runTaskAsynchronously
            }

            val perkDataMap = pp.unlockedPerkMap
            val perkFactory = ThePit.getInstance().perkFactory

            val unlockedMessages = mutableListOf<String>()

            for (p in perkFactory.perks) {
                if(p.internalPerkName.equals("hermit_bundle")) continue
                perkDataMap[p.internalPerkName] = PerkData(p.internalPerkName, p.maxLevel)
                unlockedMessages.add("${p.displayName}")
            }

            Bukkit.getScheduler().runTask(Main.instance) {
                for (msg in unlockedMessages) {
                    player.sendMessage(CC.translate("$prefix&a天赋解锁: &f$msg"))
                }
            }
        }
    }
}