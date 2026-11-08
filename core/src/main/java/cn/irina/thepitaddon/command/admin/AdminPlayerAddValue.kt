package cn.irina.thepitaddon.command.admin

import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.level.LevelUtil
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.entity.Player
import java.util.*

@Command(name = "pitAdmin add")
@Permission("pit.admin")
class AdminPlayerAddValue {
//    @Execute
//    fun addPlayerValue(
//        @Context player: Player,
//        @Arg target: Player,
//        @Arg type: String,
//        @Arg amount: Int
//    ) {
//        val profile = PlayerProfile.getRawCache(target.uniqueId)
//
//        when (type.lowercase(Locale.getDefault())) {
//            "coin" -> profile.coins += amount.toDouble()
//            "prestige" -> profile.setPrestige(profile.getPrestige() + amount)
//            "renown" -> profile.renown += amount
//            "streak" -> profile.streakKills += amount.toDouble()
//            "bounty" -> profile.bounty = profile.actionBounty + amount
//            "level" -> {
//                val levelExpRequired = LevelUtil.getLevelTotalExperience(profile.getPrestige(), amount)
//                profile.experience += levelExpRequired
//
//                profile.applyExperienceToPlayer(player)
//            }
//
//            "maxhealth" -> {
//                profile.maxHealth += amount.toDouble()
//                target.maxHealth = profile.maxHealth
//            }
//
//            "points" -> profile.genesisData.points += amount
//        }
//
//        player.sendMessage(CC.translate(Main.instance.PREFIX + "&a添加成功!"))
//    }
}
