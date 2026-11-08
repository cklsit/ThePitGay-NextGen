package cn.irina.thepitaddon.enchantment.type.recode.rare

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.irina.thepitaddon.Main
import net.minecraft.server.v1_8_R3.EntityHuman
import net.minecraft.server.v1_8_R3.ItemBow
import net.mizukilab.pit.enchantment.AbstractEnchantment
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.cooldown.Cooldown
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.scheduler.BukkitRunnable
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


@BowOnly
class VolleyA : AbstractEnchantment(), Listener {
    private val playerUsingFiled: Field = EntityHuman::class.java.getDeclaredField("h")
    private val cooldown = HashMap<UUID, Cooldown>()
    private val arrowBuilder: ItemBuilder = ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false)

    init {
        playerUsingFiled.isAccessible = true
    }

    override fun getEnchantName(): String {
        return "连续射击"
    }

    override fun getMaxEnchantLevel(): Int {
        return 3
    }

    override fun getNbtName(): String {
        return "volley_enchant"
    }

    override fun getRarity(): EnchantmentRarity {
        return EnchantmentRarity.RARE
    }

    override fun getCooldown(): Cooldown? {
        return null
    }

    override fun getUsefulnessLore(enchantLevel: Int): String {
        return "射箭时同时射出 &e${enchantLevel + 2} &7支箭矢"
    }

    private val isShooting = ConcurrentHashMap<UUID, Boolean>()
    @EventHandler
    fun onInteract(event: EntityShootBowEvent) {
        if (event.entity !is Player) return

        val player = event.entity as Player

        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return

        val itemInHand = player.itemInHand
        if (itemInHand == null || itemInHand.type != Material.BOW) return

        val level = ThePit.getApi().getItemEnchantLevel(itemInHand, this.nbtName)
        if (level <= 0) return

        if (isShooting.getOrDefault(player.uniqueId, false)) return
        event.isCancelled = true

        val nmsItem = CraftItemStack.asNMSCopy(itemInHand)
        val bow = nmsItem.item as ItemBow
        val entityPlayer = (player as CraftPlayer).handle

        val value: Int
        try {
            value = playerUsingFiled[entityPlayer] as Int
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Failed to access player field.", e)
        }

        player.inventory.addItem(arrowBuilder.amount(level + 1).build())

        isShooting[player.uniqueId] = true
        object : BukkitRunnable() {
            var tick: Int = 0

            override fun run() {
                val profile = PlayerProfile.getRawCache(player.uniqueId)
                if (tick > level + 1 || !profile.isInArena) {
                    isShooting[player.uniqueId] = false
                    cancel()
                    return
                }

                ++tick
                bow.a(nmsItem, entityPlayer.world, entityPlayer, value)
            }
        }.runTaskTimer(Main.instance, 0L, 1L)
    }
}