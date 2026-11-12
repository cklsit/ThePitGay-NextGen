package cn.irina.thepitaddon.enchantment.type.normal

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean

@WeaponOnly
class Terminator : AbstractEnchantment(), IAttackEntity {
    override fun getEnchantName(): String {
        return "终结者"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "terminator"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7对未穿着 &7" + "&f头盔 " + "&7的玩家造成的伤害 &c+" + (25 + 50 * enchantLevel) + "%"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        if (entity !is Player) return
        onDamage(entity, enchantLevel, boostDamage)
    }

    private fun onDamage(player: Player, enchantLevel: Int, boostDamage: AtomicDouble) {
        if (!notHasHelmet(player)) return
        boostDamage.getAndAdd((25 + (50 * enchantLevel)) * 0.01)
    }

    private fun notHasHelmet(player: Player): Boolean {
        val helmet = player.inventory.helmet
        return helmet == null || helmet.type.name == "AIR"
    }
}