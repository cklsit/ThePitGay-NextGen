package cn.irina.thepitaddon.enchantment.type.limit.seven

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Author Irina
 * @Date 2025/10/13 11:18
 */

@WeaponOnly
class SinsGluttony: AbstractEnchantment(), IAttackEntity {
    override fun getEnchantName(): String = "七宗罪: 暴食"
    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.OP
    override fun getCooldown(): Cooldown? = null
    override fun getMaxEnchantLevel(): Int = 3
    override fun getNbtName(): String = "sins_gluttony"
    
    private fun getMinimumHealthPercentage(i: Int): Int = 30 + (i * 10)
    private fun getRestoreHealthPercentage(i: Int): Int = 10 + (i * 15)
    
    override fun getUsefulnessLore(i: Int): String {
        return StringBuilder().apply {
            append("&7当自身血量低于最大血量的 &c${getMinimumHealthPercentage(i)}% &7时, 自身将进入 &c进食状态 /s")
            append("&c进食状态 &7下攻击非Bot目标时, 将会恢复自身数值为目标当前血量的 &c${getRestoreHealthPercentage(i)}% &7血量 /s")
            append("&7同时, 自身造成的伤害将降低 &9-20%")
        }.toString()
    }
    
    override fun handleAttackEntity(i: Int, attacker: Player, entity: Entity, p3: Double, p4: AtomicDouble?, boost: AtomicDouble, p6: AtomicBoolean?) {
        val target = entity as? Player ?: return
        if (target.hasMetadata("NPC")) return
        
        val (iHealth, iMaxHealth) = attacker.health to attacker.maxHealth
        if (!isBelowHealth(iHealth, iMaxHealth, i)) return
        PlayerUtil.heal(attacker, getRestoreHealthValue(target.health, i))
        boost.getAndAdd(-0.2)
    }
    
    private fun getRestoreHealthValue(nowHealth: Double, level: Int): Double = (nowHealth * 0.01) * getRestoreHealthPercentage(level)
    private fun isBelowHealth(nowHealth: Double, maxHealth: Double, level: Int): Boolean {
        val numerical = (maxHealth * 0.01) * getMinimumHealthPercentage(level)
        return nowHealth <= numerical
    }
}