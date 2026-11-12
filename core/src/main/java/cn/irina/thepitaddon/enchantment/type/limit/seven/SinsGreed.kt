package cn.irina.thepitaddon.enchantment.type.limit.seven

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * @Author Irina
 * @Date 2025/10/13 19:39
 */

@WeaponOnly
@BowOnly
class SinsGreed: AbstractEnchantment(), IPlayerKilledEntity {
    override fun getEnchantName(): String = "七宗罪: 贪婪"
    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.OP
    override fun getCooldown(): Cooldown? = null
    override fun getMaxEnchantLevel(): Int = 3
    override fun getNbtName(): String = "sins_greed"
    
    private fun getRewardPercentage(i: Int): Int = 100 + (i * 80)
    
    override fun getUsefulnessLore(i: Int): String {
        return StringBuilder().apply {
            append("&7击杀时获得的 &6金币 &7与 &b经验 &7将 &cx${getRewardPercentage(i)}% /s")
            append("&7但同时, 每次击杀都会扣除自身 &c0.5❤")
        }.toString()
    }
    
    override fun handlePlayerKilled(i: Int, killer: Player, entity: Entity, coin: AtomicDouble, experience: AtomicDouble) {
        val percentage = getRewardPercentage(i) / 100.0
        coin.getAndAdd(percentage)
        experience.getAndAdd(percentage)
        
        PlayerUtil.damage(killer, PlayerUtil.DamageType.TRUE, 1.0, false)
    }
}