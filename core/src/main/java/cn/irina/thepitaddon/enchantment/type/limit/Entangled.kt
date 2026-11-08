package cn.irina.thepitaddon.enchantment.type.limit

import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.Main
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.IActionDisplayEnchant
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerShootEntity
import net.mizukilab.pit.util.cooldown.Cooldown
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/*
 * @Author Irina
 * @Date 2025/9/4 22:48
 */

@BowOnly
class Entangled: AbstractEnchantment(), IPlayerShootEntity, IActionDisplayEnchant {
    override fun getEnchantName(): String {
        return "缠"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "entangled"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(i: Int): String {
        return "&7箭矢命中目标后, 将有 &e25% &7的概率在目标脚底生成1个 &f蜘蛛网&7, /s" +
                "&7持续　&e${i * 2}s &7后消失 (${getCd(i)}s冷却)"
    }

    private fun getCd(i: Int): Int { return if (i >= 3) 10 else 20 - (i * 3) }

    private val instance = Main.instance
    private val cooldown = ConcurrentHashMap<UUID, Cooldown>()
    override fun handleShootEntity(i: Int, player: Player, entity: Entity, p3: Double, p4: AtomicDouble?, p5: AtomicDouble?, p6: AtomicBoolean?) {
        val cd = cooldown[player.uniqueId] ?: Cooldown(0L)
        if (!cd.hasExpired()) return
        cooldown[player.uniqueId] = Cooldown(getCd(i).toLong(), TimeUnit.SECONDS)

        val target = entity as? Player ?: return
        val tp = PlayerProfile.getRawCache(target.uniqueId)
        if (!target.isOnline || target.hasMetadata("NPC") || !tp.isInArena) return

        val targetLoc = target.location.clone().add(0.0, 0.5, 0.0)
        val oldBlock = if (targetLoc.block.type == Material.WEB) Material.AIR else targetLoc.block.type
        targetLoc.block.type = Material.WEB

        Bukkit.getScheduler().runTaskLater(instance, { targetLoc.block.type = oldBlock }, 20L * (i * 2))
    }

    override fun getText(p0: Int, p: Player): String {
        return getCooldownActionText(cooldown[p.uniqueId] ?: Cooldown(0L))
    }
}