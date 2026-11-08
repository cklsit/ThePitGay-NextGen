package cn.irina.thepitaddon.enchantment.type.normal

import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IAttackEntity
import net.mizukilab.pit.parm.listener.IPlayerBeKilledByEntity
import net.mizukilab.pit.parm.listener.ITickTask
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import com.google.common.util.concurrent.AtomicDouble
import net.minecraft.server.v1_8_R3.EnumParticle
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@WeaponOnly
class Revengeance : AbstractEnchantment(),  IAttackEntity, IPlayerBeKilledByEntity, IActionDisplayEnchant,
    ITickTask {
    override fun getEnchantName(): String {
        return "复仇"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "revengeance"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.NORMAL
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "&7对上一个将你击杀的玩家造成 &c" + getBoostDamage(enchantLevel) + "% &7的额外伤害 /s" +
                "&7(持有附着此附魔的&e神话之剑&7死亡时将更新击杀者)"
    }

    override fun handleAttackEntity(
        enchantLevel: Int,
        attacker: Player,
        entity: Entity?,
        v: Double,
        atomicDouble: AtomicDouble?,
        boostDamage: AtomicDouble,
        atomicBoolean: AtomicBoolean?
    ) {
        if (entity !is Player) return
        val target = entity

        val killerUuid: UUID? = lastKillerUID.get(attacker.uniqueId)
        if (killerUuid == null) return

        if (killerUuid != target.uniqueId) return

        boostDamage.getAndAdd(getBoostDamage(enchantLevel) * 0.01)
    }

    override fun handlePlayerBeKilledByEntity(
        i: Int,
        player: Player,
        entity: Entity?,
        atomicDouble: AtomicDouble?,
        atomicDouble1: AtomicDouble?
    ) {
        if (entity !is Player) return
        val killer = entity

        val kp = PlayerProfile.getRawCache(killer.uniqueId)
        if (kp == null) return

        lastKillerName.put(player.uniqueId, CC.translate("&e击杀者: " + kp.formattedNameWithRoman))
        lastKillerUID.put(player.uniqueId, killer.uniqueId)
    }

    private fun getBoostDamage(enchantLevel: Int): Int {
        return (if (enchantLevel >= 2) (5 + ((enchantLevel - 1) * 10)) else 8)
    }

    override fun getText(i: Int, player: Player): String? {
        return lastKillerName.getOrDefault(player.uniqueId, CC.translate("&e击杀者: &c无"))
    }

    override fun handle(i: Int, player: Player) {
        val killerUuid: UUID? = lastKillerUID.get(player.uniqueId)
        if (killerUuid == null) return

        val killer = Bukkit.getPlayer(killerUuid)
        if (killer == null) return

        sendRedstoneParticle(player, killer, 255f, 0f, 0f)
    }

    override fun loopTick(i: Int): Int {
        return 5
    }

    companion object {
        private val lastKillerName = HashMap<UUID?, String?>()
        private val lastKillerUID = HashMap<UUID?, UUID?>()

        fun sendRedstoneParticle(sender: Player, target: Player, r: Float, g: Float, b: Float) {
            val targetHeadLocation = target.location.add(0.0, 2.5, 0.0)

            val packet = PacketPlayOutWorldParticles(
                EnumParticle.REDSTONE,  // 固定为红石粒子
                false,
                targetHeadLocation.x.toFloat(),
                targetHeadLocation.y.toFloat(),
                targetHeadLocation.z.toFloat(),
                r,
                g,
                b,
                2.0f,
                0,
                10
            )

            // 发送给目标玩家（或其他指定玩家）
            (sender as CraftPlayer).handle.playerConnection.sendPacket(packet)
        }
    }
}
