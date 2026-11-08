package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerDamaged
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import cn.irina.thepitaddon.FixListeners
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

import java.util.concurrent.atomic.AtomicBoolean

@ArmorOnly
class ComboRadiantGold : AbstractEnchantment(), IAttackEntity, IPlayerDamaged, IActionDisplayEnchant {
    private val limitAbsorptionHearts = FixListeners.LimitAbsorptionHearts

    override fun getEnchantName(): String {
        return "强力击: 耀金"
    }

    override fun getMaxEnchantLevel(): Int {
        return 1
    }

    override fun getNbtName(): String {
        return "combo_radiant_gold"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.DARK_RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7每 &e6 &7次击中目标时, 将立刻恢复 &c1.0❤ &7生命值, 并获得 &64.0 生命吸收(❤) &7和 &b速度 II &f(00:03) /s" +
                "&7(被穿着附有 &6耀金 &7附魔的目标攻击时额外受到 &c0.5❤ &f真实&7伤害, 且穿着时 &6生命吸收(❤) &7效果上限 &c-15.0❤)"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        target: Entity,
        damage: Double,
        finalDamage: AtomicDouble,
        boostDamage: AtomicDouble,
        cancel: AtomicBoolean
    ) {
        var hit = 0

        if (attacker.itemInHand != null) hit = PlayerProfile.getRawCache(attacker.uniqueId).meleeHit

        if (hit % 6 == 0) {
            if (attacker.hasPotionEffect(PotionEffectType.SPEED)) attacker.removePotionEffect(PotionEffectType.SPEED)
            attacker.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 60, 1, true, false))

            val nmsPlayer = attacker as CraftPlayer
            if (nmsPlayer.handle.absorptionHearts + 8 < limitAbsorptionHearts - 30) {
                nmsPlayer.handle.absorptionHearts += 8f
            } else {
                nmsPlayer.handle.absorptionHearts = limitAbsorptionHearts - 30
            }

            PlayerUtil.heal(attacker, 2.0)
        }
    }

    override fun handlePlayerDamaged(
        i: Int,
        victim: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        atomicDouble1: AtomicDouble,
        cancel: AtomicBoolean
    ) {
        if (entity !is Player) return

        if (ThePit.getApi().getItemEnchantLevel(entity.inventory.leggings, this.nbtName) < 1) return

        PlayerUtil.damage(victim, PlayerUtil.DamageType.TRUE, 1.0, true)
    }

    override fun getText(enchantLevel: Int, player: Player): String {
        return getHitActionText(player, 6)
    }
}
