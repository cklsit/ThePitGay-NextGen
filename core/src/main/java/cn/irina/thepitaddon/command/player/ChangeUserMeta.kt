package cn.irina.thepitaddon.command.player

import net.mizukilab.pit.util.chat.CC
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.Node
import net.luckperms.api.node.types.PrefixNode
import net.luckperms.api.node.types.SuffixNode
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

@Command(name = "custom")
@Permission("irina.custom")
class ChangeUserMeta {
    private val luckPerms = LuckPermsProvider.get()

    @Execute
    fun customUserMeta(
        @Context player: Player,
        @Arg type: String,
        @Arg meta: String?
    ) {
        val user = luckPerms.userManager.getUser(player.uniqueId)

        if (user == null) {
            player.sendMessage(CC.translate("$PREFIX&c你的数据为Null!"))
            return
        }

        if (meta == null) {
            player.sendMessage(CC.translate("$PREFIX&cMeta不能为Null!"))
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
            val node: Node
            when (type.lowercase(Locale.getDefault())) {
                "suffix" -> {
                    for (suffixNode in user.nodes) {
                        if (suffixNode is SuffixNode) {
                            user.data().remove(suffixNode)
                        }
                    }
                    node = SuffixNode.builder(CC.translate("$meta&r"), 1000).build()
                }

                "prefix" -> {
                    for (prefixNode in user.nodes) {
                        if (prefixNode is PrefixNode) {
                            user.data().remove(prefixNode)
                        }
                    }
                    node = PrefixNode.builder(CC.translate("$meta&r"), 1000).build()
                }

                else -> {
                    player.sendMessage(CC.translate("$PREFIX&c错误的类型! < Prefix / Suffix >"))
                    return@runTaskAsynchronously
                }
            }

            user.data().add(node)

            luckPerms.userManager.saveUser(user)
            Bukkit.getScheduler().runTask(Main.instance) {
                player.sendMessage(
                    CC.translate(
                        "$PREFIX&a成功! 当前你的 &e" + type.uppercase(
                            Locale.getDefault()
                        ) + " &a是 " + meta
                    )
                )
            }
        }
    }

    companion object {
        private val PREFIX = Main.instance.PREFIX
    }
}
