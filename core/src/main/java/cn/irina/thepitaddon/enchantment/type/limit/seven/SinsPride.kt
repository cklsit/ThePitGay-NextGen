package cn.irina.thepitaddon.enchantment.type.limit.seven

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Author Irina
 * @Date 2025/10/13 19:11
 */

@ArmorOnly
class SinsPride: AbstractEnchantment(), IPlayerDamaged {
    override fun getEnchantName(): String = "七宗罪: 傲慢"
    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.OP
    override fun getCooldown(): Cooldown? = null
    override fun getMaxEnchantLevel(): Int = 3
    override fun getNbtName(): String = "sins_pride"
    
    override fun getUsefulnessLore(i: Int): String {
        return StringBuilder().apply {
            append("&7穿戴附有此附魔的 &e神话之甲 &7时: /s")
            append("&7遭受到非Bot目标攻击时, 若目标当前血量少于自身当前血量 /s")
            append("&7则受到的伤害减免 &9-${getDamageRemission(i)}%")
        }.toString()
    }
    
    override fun handlePlayerDamaged(i: Int, victim: Player, entity: Entity, p3: Double, p4: AtomicDouble, boost: AtomicDouble, p6: AtomicBoolean?) {
        val attacker = entity as? Player ?: return
        if (attacker.hasMetadata("NPC")) return
        
        val (iHealth, tHealth) = victim.health to attacker.health
        if (!isBelowHealth(iHealth, tHealth)) return
        boost.getAndAdd(-getDamageRemission(i).toDouble())
    }
    
    private fun getDamageRemission(i: Int): Int = 5 + (i * 10)
    private fun isBelowHealth(myHealth: Double, targetHealth: Double): Boolean = myHealth > targetHealth
}