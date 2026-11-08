package net.mizukilab.pit.npc.type.custom

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.command.player.AfkCommand
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.mizukilab.pit.npc.AbstractCustomEntityNPC
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager

/**
 * 挂机池 NPC：右键进入挂机池世界。
 */
class AfkPoolNPC : AbstractCustomEntityNPC() {

    override fun getNpcInternalName(): String {
        return "afk_pool_npc"
    }

    override fun getNpcDisplayName(player: Player): List<String> {
        val lines: MutableList<String> = ObjectArrayList(2)
        lines.add("&b&l挂机池")
        lines.add("&e右键进入挂机池")
        return lines
    }

    override fun getNpcSpawnLocation(): Location {
        return ThePit.getInstance().pitConfig.afkPoolNpcLocation
    }

    override fun getEntityType(): EntityType {
        return EntityType.VILLAGER
    }

    override fun handlePlayerInteract(player: Player) {
        AfkCommand.enterAfk(player)
    }

    override fun customizeEntity(entity: Entity) {
        if (entity is Villager) {
            entity.profession = Villager.Profession.NITWIT
            entity.removeWhenFarAway = false
            entity.setAI(false)
            entity.setSilent(true)
            entity.setAdult()
        }
    }
}
