package cn.irina.thepitaddon.enchantment.type.rare

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.PitItem
import com.google.common.util.concurrent.AtomicDouble
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.event.PlayerOnly
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.IPlayerKilledEntity
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.random.RandomUtil
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

@ArmorOnly
class MinersEnchant : AbstractEnchantment(), IPlayerKilledEntity {
    private val guaranteedReward = mutableMapOf<UUID, Boolean>()

    override fun getEnchantName(): String {
        return "矿工"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "miners_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }


    override fun getUsefulnessLore(i: Int): String {
        return "&7每击杀 &e1000 &7名玩家有 &e" + (i + 2) * 10 + "% &7的概率获得 &f1x &5暗聚块."
    }

    @PlayerOnly
    override fun handlePlayerKilled(
        enchantLevel: Int,
        myself: Player,
        target: Entity,
        coins: AtomicDouble,
        experience: AtomicDouble
    ) {
        val profile = PlayerProfile.getPlayerProfileByUuid(myself.uniqueId)
        val streakKills = profile.streakKills
        if (streakKills.toInt() % 1000 != 0 || streakKills == 0.0) return
        val pitItem = PitItem()
        val chance = 0.01 * (enchantLevel + 2) * 10
        val playerId = myself.uniqueId

        if (guaranteedReward.getOrDefault(playerId, false)) {
            myself.inventory.addItem(pitItem.chunkOfVile)
            playSound(myself)
            myself.sendMessage(CC.translate("&9&l矿工! &7你获得了 &f1x &5暗聚块."))
            guaranteedReward[playerId] = false
        } else if (RandomUtil.hasSuccessfullyByChance(chance)) {
            myself.inventory.addItem(pitItem.chunkOfVile)
            playSound(myself)
            myself.sendMessage(CC.translate("&9&l矿工! &7你获得了 &f1x &5暗聚块."))
            guaranteedReward[playerId] = false
        } else {
            myself.sendMessage(CC.translate("&9&l矿工! &7看起来你的运气不太好...下次一定!"))
            guaranteedReward[playerId] = true
        }
    }

    private fun playSound(player: Player) {
        object : BukkitRunnable() {
            var tick: Int = 0
            var shouldUp: Boolean = false
            override fun run() {
                player.playSound(player.location, Sound.NOTE_PLING, 20f, 0.6f + (tick * 0.05f))
                if (shouldUp) {
                    tick++
                }
                if (!shouldUp) {
                    shouldUp = true
                }
                if (tick >= 4) {
                    cancel()
                }
            }
        }.runTaskTimer(ThePit.getInstance(), 4, 4)
    }
}