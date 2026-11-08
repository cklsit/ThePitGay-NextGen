package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

@WeaponOnly
@ArmorOnly
class KillAngels : AbstractEnchantment(),  IPlayerKilledEntity, IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "杀戮天使"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "kill_angels"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7每当你连杀 &c25 &7位目标, 你的&3经验&7和&6金币&7奖励 &b+" + enchantLevel * 0.5 + "% &7(上限&e400&7层)"
    }

    override fun handlePlayerKilled(
        enchantLevel: Int,
        killer: Player,
        entity: Entity,
        coins: AtomicDouble,
        experience: AtomicDouble
    ) {
        val streak = checkStreak(killer)
        val boostReward = 1 + (streak * (enchantLevel * 0.005))
        coins.set(coins.get() * boostReward)
        experience.set(experience.get() * boostReward)
    }

    override fun getText(enchantLevel: Int, killer: Player): String {
        val streak = checkStreak(killer)
        return CC.translate("&f&k!!&e 加成: " + streak * (enchantLevel * 0.5) + "% &f| &e层数: " + streak + " &f&k!!")
    }

    private fun checkStreak(killer: Player): Int {
        val kp = PlayerProfile.getRawCache(killer.uniqueId) ?: return 0
        return if (kp.streakKills.toInt() / 25 >= 400) 400 else kp.streakKills.toInt() / 25
    }
}
