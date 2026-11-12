package cn.irina.thepitaddon.enchantment.type.rare

import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/*
 * @Author Irina
 * @Date 2025/5/2 13:03
 */

@WeaponOnly
class Stifle: AbstractEnchantment(), IAttackEntity {
    override fun getEnchantName(): String? {
        return "扼杀"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String? {
        return "stifle"
    }

    override fun getRarity(): EnchantmentRarity? {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String? {
        return "&7攻击目标时, 目标每损失 &c1% &7的血量, 将额外对目标造成 &c1% &7的伤害 &7(上限&e${enchantLevel * 25}&7层)"
    }

    private val maxHealthCache = ConcurrentHashMap<UUID, Double>()
    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        entity: Entity,
        p3: Double,
        p4: AtomicDouble,
        boostDamage: AtomicDouble,
        p6: AtomicBoolean,
    ) {
        val target = entity as? Player ?: return
        val targetUuid = target.uniqueId

        if (maxHealthCache[targetUuid] == null || maxHealthCache[targetUuid] != target.maxHealth) maxHealthCache[targetUuid] = target.maxHealth

        val perCent = maxHealthCache[targetUuid]!! * 0.01
        val lostHealth = maxHealthCache[targetUuid]!! - target.health

        val damageCount = (enchantLevel * 25).coerceAtMost((lostHealth / perCent).toInt())
        boostDamage.getAndAdd(damageCount * 0.01)
    }
}