package net.mizukilab.pit.npc.type.custom

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.merchant.SpacetimeMerchantMenu
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.mizukilab.pit.npc.AbstractCustomEntityNPC
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager

/**
 * 时空商人 NPC：出售强化钻石剑、铁头盔、各类功能蛋与实体货币。
 */
class SpacetimeMerchantNPC : AbstractCustomEntityNPC() {

    override fun getNpcInternalName(): String {
        return "spacetime_merchant_npc"
    }

    override fun getNpcDisplayName(player: Player): List<String> {
        val lines: MutableList<String> = ObjectArrayList(2)
        lines.add("&5&l时空商人")
        lines.add("&e右键打开商店")
        return lines
    }

    override fun getNpcSpawnLocation(): Location {
        return ThePit.getInstance().pitConfig.spacetimeMerchantNpcLocation
    }

    override fun getEntityType(): EntityType {
        return EntityType.VILLAGER
    }

    override fun handlePlayerInteract(player: Player) {
        SpacetimeMerchantMenu().openMenu(player)
    }

    override fun customizeEntity(entity: Entity) {
        if (entity is Villager) {
            entity.profession = Villager.Profession.CARTOGRAPHER
            entity.removeWhenFarAway = false
            entity.setAI(false)
            entity.setSilent(true)
            entity.setAdult()
        }
    }
}
