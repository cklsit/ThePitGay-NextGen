package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.ThePit
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
 * @Creator Misoryan
 * @Date 2021/5/8 14:12
 */
@ArmorOnly
class ThinkOfThePeopleEnchant : AbstractEnchantment(), IPlayerDamaged {

    private val pitAPI = ThePit.getApi()

    override fun getEnchantName(): String {
        return "\"为人着想\""
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "think_of_the_people"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RAGE_RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return ("&7为自身添加以下效果:"
                + "/s  &f▶ &7免疫附魔 &c狂暴连击 &7的效果"
                + if (enchantLevel > 1) "/s&7受到来自以上附魔使用者的伤害 &9-" + (enchantLevel * 10 - 10) + "%" else "")
    }

    override fun handlePlayerDamaged(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        p3: Double,
        p4: AtomicDouble?,
        reduceDamage: AtomicDouble,
        p6: AtomicBoolean?
    ) {
        val attacker = entity as? Player ?: return
        if (enchantLevel <= 1 || pitAPI.getItemEnchantLevel(attacker.inventory.leggings, "regularity") <= 0) return
        reduceDamage.set(reduceDamage.get() * (1 - enchantLevel * 0.1 + 0.1))
    }
}