package cn.irina.thepitaddon.enchantment.type.dj

import cn.irina.thepitaddon.Main.OverallResourceHolder.instance
import cn.irina.thepitaddon.utils.SongUtil
import net.minecraft.server.v1_8_R3.PacketPlayInFlying
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.parm.listener.ITickTask
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.music.NBSDecoder
import net.mizukilab.pit.util.music.PositionSongPlayer
import net.mizukilab.pit.util.music.Song
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import spg.lgdev.handler.MovementHandler
import spg.lgdev.iSpigot
import java.util.*

@ArmorOnly
class DJ15 : AbstractEnchantment(), ITickTask, MovementHandler {

    private val playerMap: MutableMap<UUID, PositionSongPlayer> = HashMap()
    private val music: Song = NBSDecoder.parse(instance.javaClass.getClassLoader().getResourceAsStream("Megalovania.nbs"))

    init {

        object : BukkitRunnable() {
            override fun run() {
                for (entry in HashSet(playerMap.entries)) {
                    val player: Player? = Bukkit.getPlayer(entry.key)

                    if (player == null || !player.isOnline || player.inventory.leggings == null || getItemEnchantLevel(player.inventory.leggings) <= 0) {
                        val player = playerMap.remove(entry.key)
                        player?.isPlaying = false
                    }
                }
            }
        }.runTaskTimerAsynchronously(instance, 20, 20)

        try {
            iSpigot.INSTANCE.addMovementHandler(this)
        } catch (ignore: NoClassDefFoundError) {
        }
    }

    override fun getEnchantName(): String {
        return "DJ #15"
    }

    override fun getMaxEnchantLevel(): Int {
        return 1
    }

    override fun getNbtName(): String {
        return "megalovania_dj"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.OP
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return ("&7此附魔只能通过 &e抽奖活动 &7获得." + "/s&7向周围的玩家播放音乐: &fMegalovania")
    }

    override fun handle(enchantLevel: Int, target: Player) {
        SongUtil.songPlay(target, playerMap, music)
    }

    override fun loopTick(enchantLevel: Int): Int {
        return 10
    }

    override fun handleUpdateLocation(
        player: Player,
        location: Location?,
        location1: Location?,
        packetPlayInFlying: PacketPlayInFlying?
    ) {
        val songPlayer = this.playerMap[player.uniqueId]
        songPlayer?.targetLocation = player.player.location
    }

    override fun handleUpdateRotation(
        player: Player?,
        location: Location?,
        location1: Location?,
        packetPlayInFlying: PacketPlayInFlying?
    ) {
    }
}
