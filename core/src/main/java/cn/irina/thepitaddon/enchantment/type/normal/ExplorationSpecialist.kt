package cn.irina.thepitaddon.enchantment.type.normal

import cn.charlotte.pit.ThePit
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.chat.RomanUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.utils.LocationUtil
import net.minecraft.server.v1_8_R3.PacketPlayInFlying
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import spg.lgdev.handler.MovementHandler

@ArmorOnly
class ExplorationSpecialist : AbstractEnchantment(),  MovementHandler{
    override fun getEnchantName(): String {
        return "深海探索者"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "exploration_specialist"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.FISH_NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7自身处于水中时, 将持续获得以下效果: /s" +
                "   &f▶ &b速度 ${RomanUtil.convert(enchantLevel)} /s" +
                "   &f▶ &c生命恢复 ${RomanUtil.convert(enchantLevel)} /s" +
                "&7同时, 移除自身 &8缓慢 &7效果"
    }

    override fun handleUpdateLocation(
        player: Player,
        p1: Location?,
        p2: Location?,
        p3: PacketPlayInFlying?
    ) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.instance) {
            val enchantLevel = ThePit.getApi().getItemEnchantLevel(player.inventory.leggings, this.nbtName)

            if (enchantLevel < 1) return@runTaskAsynchronously
            if (!LocationUtil.isInWater(player)) return@runTaskAsynchronously

            if (player.hasPotionEffect(PotionEffectType.SLOW)) player.removePotionEffect(PotionEffectType.SLOW)
            if (player.hasPotionEffect(PotionEffectType.SPEED)) player.removePotionEffect(PotionEffectType.SPEED)
            if (player.hasPotionEffect(PotionEffectType.REGENERATION)) player.removePotionEffect(PotionEffectType.REGENERATION)

            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20, enchantLevel - 1, false, true))
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20, enchantLevel - 1, false, true))
        }
    }

    override fun handleUpdateRotation(
        p0: Player?,
        p1: Location?,
        p2: Location?,
        p3: PacketPlayInFlying?
    ) {
        
    }
}