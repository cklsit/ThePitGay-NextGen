package cn.irina.thepitaddon.command.admin

import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.mizukilab.pit.util.chat.CC
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.UUID

@Command(name = "god")
@Permission("irina.staff")
class GodMode : Listener {
//    private val prefix = Main.instance.PREFIX
//    private val activeGodPlayers: MutableSet<UUID> = mutableSetOf()
//
//    private val activeMessage = CC.translate("$prefix&c你已开启God模式, 将不被允许 攻击 & 受击!")
//
//    @Execute
//    fun onControlGodMode(@Context sender: CommandSender) {
//        if (sender !is Player) {
//            sender.sendMessage(CC.translate("$prefix&c此命令只可由玩家执行!"))
//            return
//        }
//
//        val player = sender
//
//        if (checkPlayerIsActive(player.uniqueId)) {
//            activeGodPlayers.remove(player.uniqueId)
//            player.allowFlight = false
//            player.sendMessage(CC.translate("$prefix&c你已关闭God模式"))
//            return
//        }
//
//        activeGodPlayers.add(player.uniqueId)
//        player.allowFlight = true
//        player.sendMessage(CC.translate("$prefix&a你已开启God模式"))
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onAttack(event: EntityDamageByEntityEvent) {
//        if (event.damager !is Player) return
//        val attacker = event.damager as Player
//
//        if (!checkPlayerIsActive(attacker.uniqueId)) return
//        event.isCancelled = true
//        attacker.sendMessage(activeMessage)
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onProtect(event: EntityDamageByEntityEvent) {
//        if (event.entity !is Player) return
//        val victim = event.entity as Player
//
//        if (!checkPlayerIsActive(victim.uniqueId)) return
//        event.isCancelled = true
//        victim.sendMessage(activeMessage)
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onProjectDamage(event: EntityDamageByEntityEvent) {
//        val attacker = event.damager
//        if (attacker !is Projectile || attacker.shooter !is Player) return
//
//        val shooter = attacker.shooter
//        if (shooter !is Player) return
//
//        if (!checkPlayerIsActive(shooter.uniqueId)) return
//        event.isCancelled = true
//        shooter.sendMessage(activeMessage)
//    }
//
//    fun checkPlayerIsActive(uuid: UUID): Boolean {
//        for (pu in activeGodPlayers) {
//            if (pu == uuid) return true
//        }
//
//        return false
//    }
}