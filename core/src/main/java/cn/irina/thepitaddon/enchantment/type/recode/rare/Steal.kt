package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.data.PlayerProfile
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicBoolean

@WeaponOnly
class Steal : AbstractEnchantment(), IAttackEntity, IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "强力击: 窃取"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "steal"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7每 &e4 &7次击中目标时: /s" +
                "&7对敌方造成 &f${getDamage(enchantLevel)}❤ 必中伤害 /s" +
                "&7并恢复自身 &c${getDamage(enchantLevel)}❤ 生命值"
    }

    private fun getDamage(enchantLevel: Int): Double {
        return enchantLevel.toDouble() / 2
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        entity: Entity,
        p3: Double,
        p4: AtomicDouble?,
        p5: AtomicDouble?,
        p6: AtomicBoolean?
    ) {
//        if (RandomUtil().hasSuccessfullyByChance(0.5)) {
//            PlayerUtil.damage(attacker, PlayerUtil.DamageType.TRUE, getDamage(enchantLevel), false)
//            attacker.sendMessage(CC.translate("&4&l赌徒! &c你赌输了。"))
//            return
//        }
//
//        val target = entity as? Player ?: return
//        PlayerUtil.damage(target, PlayerUtil.DamageType.TRUE, getDamage(enchantLevel), false)
//        target.sendMessage(CC.translate("&4&l赌徒! &c你受到来自对方的 &f" + (getDamage(enchantLevel) / 2) + "❤ &c必中伤害."))
//        attacker.sendMessage(CC.translate("&2&l赌徒! &a对目标 &f${target.name} &a造成了伤害!"))

        var hit = 0

        if (attacker.itemInHand != null) hit = PlayerProfile.getRawCache(attacker.uniqueId).meleeHit

        val victim = entity as? Player ?: return
        if (hit % 2 != 0) return
        PlayerUtil.damage(victim, PlayerUtil.DamageType.TRUE, getDamage(enchantLevel) * 2, false)
        PlayerUtil.heal(attacker, getDamage(enchantLevel) * 2)
    }

    override fun getText(p0: Int, p1: Player?): String {
        return getHitActionText(p1, 4)
    }
}