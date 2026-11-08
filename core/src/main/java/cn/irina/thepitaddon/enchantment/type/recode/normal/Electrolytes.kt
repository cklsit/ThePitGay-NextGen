package cn.irina.thepitaddon.enchantment.type.recode.normal

import cn.irina.thepitaddon.manager.PitManager
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.event.PlayerOnly
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

/*
 * @Author Irina
 * @Date 2025/7/3 01:19
 */

@ArmorOnly
class Electrolytes : AbstractEnchantment(), IPlayerKilledEntity {
    override fun getEnchantName(): String {
        return "电解质"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "electrolytes_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return ("&7击杀时如自身存在 &b速度 &7效果,延长效果时间 &e" + (enchantLevel * 2) + " 秒"
                + "/s&7(如效果等级大于II则延长时间减半,上限" + ((enchantLevel + 2) * 6) + "秒)")
    }

    @PlayerOnly
    override fun handlePlayerKilled(
        enchantLevel: Int,
        myself: Player,
        target: Entity?,
        coins: AtomicDouble?,
        experience: AtomicDouble?
    ) {
        if (!myself.hasPotionEffect(PotionEffectType.SPEED)) return
        if (PlayerUtil.isVenom(myself)) return

        for (p in myself.activePotionEffects) {
            if (p.type != PotionEffectType.SPEED) continue

            val duration = if (p.amplifier > 1)
                p.duration + (enchantLevel * 20)
            else
                p.duration + (enchantLevel * 2 * 20)

            myself.removePotionEffect(p.type)
            val maxDuration = (enchantLevel + 2) * 6 * 20
            val finalDuration = if (duration >= maxDuration) maxDuration else duration
            PitManager.givePlayerPotionEffect(
                myself,
                PotionEffectType.SPEED,
                finalDuration,
                p.amplifier
            )
        }
    }
}