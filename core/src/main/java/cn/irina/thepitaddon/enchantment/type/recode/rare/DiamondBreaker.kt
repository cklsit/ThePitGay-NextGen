package cn.irina.thepitaddon.enchantment.type.recode.rare

import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@ArmorOnly
@WeaponOnly
@BowOnly
class DiamondBreaker : AbstractEnchantment(),  IAttackEntity, IPlayerShootEntity {
    override fun getEnchantName(): String {
        return "钻石破坏者"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "diamond_breaker"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7攻击穿着 &b钻石装备 &7的玩家造成的伤害 &c+${5 + (enchantLevel * 5)}%"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        v: Double,
        atomicDouble: AtomicDouble,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean
    ) {
        onDamage(entity, enchantLevel, boostDamage)
    }

    override fun handleShootEntity(
        enchantLevel: Int,
        player: Player,
        entity: Entity,
        p3: Double,
        p4: AtomicDouble,
        boostDamage: AtomicDouble,
        p6: AtomicBoolean
    ) {
        onDamage(entity, enchantLevel, boostDamage)
    }

    private fun onDamage(entity: Entity, enchantLevel: Int, boostDamage: AtomicDouble) {
        if (entity !is Player) return

        if (!isDiamondItem(entity)) return

        boostDamage.getAndAdd((5 + (enchantLevel * 5)) * 0.01)

    }

    private fun isDiamondItem(player: Player): Boolean {
        val armor = player.inventory.armorContents

        for (item in armor) {
            if (diamondArmorTypes.contains(item.type)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val diamondArmorTypes: Set<Material> = EnumSet.of(
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS
        )
    }
}
