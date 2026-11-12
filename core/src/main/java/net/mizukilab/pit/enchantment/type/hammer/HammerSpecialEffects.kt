package net.mizukilab.pit.enchantment.type.hammer

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.compat.item.ItemTypes
import cn.charlotte.pit.data.PlayerProfile
import net.mizukilab.pit.enchantment.seasonal.SeasonalOpenService
import net.mizukilab.pit.item.IMythicItem
import net.mizukilab.pit.util.chat.CC
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 神话之锤 SPECIAL 类附魔处理器（第一批）。
 *
 * 覆盖：13 闷响 / 27 楔入 / 11 夯土 / 29 地鸣 / 52 铁砂掌 / 30 轨痕 / 76 反锻 /
 *       50 秤星 / 43 回炉 / 56 铁账 / 49 补锭 / 80 铸魂。
 */
class HammerSpecialEffects : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val item = attacker.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        if (mythic.enchantments.isEmpty()) return
        val target = event.entity as? LivingEntity ?: return

        val levelOf = { nbt: String ->
            mythic.enchantments.entries.firstOrNull {
                it.key is HammerEnchantBase && it.key.nbtName == nbt && SeasonalOpenService.isOpen(it.key)
            }?.value ?: 0
        }

        // 13 闷响：减速
        levelOf("hammer_013").takeIf { it > 0 }?.let { lv ->
            val seconds = (1.0 + (lv - 1) * 0.5).toInt().coerceAtLeast(1)
            target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, 1, false, true))
        }

        // 27 楔入：禁止冲刺
        levelOf("hammer_027").takeIf { it > 0 && target is Player }?.let {
            (target as Player).isSprinting = false
        }

        // 11 夯土 / 29 地鸣：击退
        val knockLevel = maxOf(levelOf("hammer_011"), levelOf("hammer_029"))
        if (knockLevel > 0) {
            val push = 0.35 + (knockLevel - 1) * 0.1
            val dir = target.location.toVector().subtract(attacker.location.toVector()).setY(0)
            if (dir.lengthSquared() > 0.0001) {
                dir.normalize().multiply(push).setY(0.25)
                target.velocity = dir
            }
        }

        // 52 铁砂掌：向自身拉回
        levelOf("hammer_052").takeIf { it > 0 }?.let { lv ->
            val pull = 0.3 + (lv - 1) * 0.15
            val dir = attacker.location.toVector().subtract(target.location.toVector()).setY(0)
            if (dir.lengthSquared() > 0.0001) {
                dir.normalize().multiply(pull)
                target.velocity = dir
            }
        }

        // 30 轨痕：冲刺后命中前冲
        if (levelOf("hammer_030") > 0 && attacker.isSprinting) {
            val dir = attacker.location.direction.clone().setY(0)
            if (dir.lengthSquared() > 0.0001) {
                attacker.velocity = dir.normalize().multiply(0.55).setY(0.15)
            }
        }

        // 50 秤星：ActionBar 显示 live 比例
        levelOf("hammer_050").takeIf { it > 0 }?.let {
            val live = mythic.live
            val max = mythic.maxLive.coerceAtLeast(1)
            attacker.sendActionBar(CC.translate("&7生命: &e$live&7/&e$max &8(${live * 100 / max}%)"))
        }

        // 43 回炉：每 8 击回复 live
        val counter = HIT_COUNTER.merge(attacker.uniqueId, 1, Int::plus) ?: 1

        levelOf("hammer_043").takeIf { it > 0 && counter % 8 == 0 }?.let { lv ->
            restoreLive(attacker, mythic, if (lv >= 3) 2 else 1)
        }

        // 56 铁账：每 10 击获得硬币
        levelOf("hammer_056").takeIf { it > 0 && counter % 10 == 0 }?.let { lv ->
            val coins = 5.0 * lv
            val profile = PlayerProfile.getRawCache(attacker.uniqueId)
            if (profile != null) {
                profile.coins = profile.coins + coins
                attacker.sendMessage(CC.translate("&6&l铁账! &7获得 &6$coins &7硬币"))
            }
        }
    }

    /** 76 反锻：受近战伤害反弹。 */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onReflect(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return
        val item = victim.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == "hammer_076" && SeasonalOpenService.isOpen(it.key)
        } ?: return
        val pct = (entry.key as HammerEnchantBase).valueAt(entry.value).coerceAtMost(40.0)
        val reflect = event.damage * (pct / 100.0)
        if (reflect <= 0.0) return
        attacker.damage(reflect, victim)
    }

    /** 49 补锭 / 80 铸魂：击杀回复 live。 */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onKill(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val item = killer.inventory.itemInMainHand
        if (!ItemTypes.isMythicHammer(item)) return
        val mythic = ThePit.getInstance().itemFactory.getItemFromStack(item) as? IMythicItem ?: return
        val isPlayerVictim = event.entity is Player

        val targetNbt = if (isPlayerVictim) "hammer_080" else "hammer_049"
        val entry = mythic.enchantments.entries.firstOrNull {
            it.key is HammerEnchantBase && it.key.nbtName == targetNbt && SeasonalOpenService.isOpen(it.key)
        } ?: return
        val amount = (entry.key as HammerEnchantBase).valueAt(entry.value).toInt().coerceAtLeast(1)
        restoreLive(killer, mythic, amount)
        killer.playSound(killer.location, Sound.BLOCK_ANVIL_USE, 1f, 1f)
    }

    private fun restoreLive(player: Player, mythic: IMythicItem, amount: Int) {
        val max = mythic.maxLive
        if (max <= 0) return
        val next = (mythic.live + amount).coerceAtMost(max)
        if (next == mythic.live) return
        mythic.live = next
        player.inventory.setItemInMainHand(mythic.toItemStack())
    }

    companion object {
        private val HIT_COUNTER = ConcurrentHashMap<UUID, Int>()
    }
}