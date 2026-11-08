package cn.irina.thepitaddon.manager

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.PlayerProfile
import cn.charlotte.pit.data.sub.EnchantmentRecord
import dev.rollczi.litecommands.annotations.context.Context
import net.mizukilab.pit.util.PlayerUtil
import net.mizukilab.pit.util.item.ItemUtil
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * @Author ShanguanLinG
 * @Date 2025/8/07
 * @Assist Irina
 */

object PitManager {
    private val pitApi = ThePit.api

    @JvmStatic
    fun hasInternalName(item: ItemStack, internalName: String): Boolean {
        return internalName == getInternalName(item)
    }

    @JvmStatic
    fun getInternalName(item: ItemStack): String {
        return ItemUtil.getInternalName(item) ?: ""
    }

    @JvmStatic
    fun hasPitEnchant(item: ItemStack, enchantName: String): Boolean {
        return getPitEnchantLevel(item, enchantName) > 0
    }

    @JvmStatic
    fun getPitEnchantLevel(item: ItemStack, enchantName: String): Int {
        return pitApi.getItemEnchantLevel(item, enchantName)
    }

    @JvmStatic
    fun getAbsorptionHearts(player: Player): Float {
        val craftPlayer = player as CraftPlayer
        return craftPlayer.handle.absorptionHearts
    }

    fun hasAbsolutionHearts(player: Player): Boolean {
        val absorptionHearts = getAbsorptionHearts(player)
        return absorptionHearts > 0
    }

    @JvmStatic
    fun isNPC(player: Player): Boolean {
        return PlayerUtil.isNPC(player) || "bot" == player.name || player.hasMetadata("NPC")
    }

    @JvmStatic
    fun takeInternalItem(player: Player, internalName: String, count: Int) {
        var remaining = count
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (remaining <= 0) break
            val item = inventory.getItem(slot) ?: continue
            if (!hasInternalName(item, internalName)) continue
            val takeAmount = minOf(item.amount, remaining)
            if (item.amount > takeAmount) {
                item.amount -= takeAmount
                inventory.setItem(slot, item)
            } else {
                inventory.setItem(slot, null)
            }
            remaining -= takeAmount
        }
        flushPlayerItem(player)
    }

    @JvmStatic
    fun getInternalItemAmount(player: Player, internalName: String): Int {
        var amount = 0
        for (item in player.inventory) {
            if (item == null || item.type == Material.AIR) continue
            if (!hasInternalName(item, internalName)) continue
            amount += item.amount
        }
        return amount
    }


    /**
     * 智能判断玩家身上的Buff等级与时长
     *
     * 判断逻辑:
     * 1. 玩家身上没有该效果 -> 直接给予效果
     * 2. 玩家身上有该效果, 但等级低于被给予值 -> 清除原效果重新施加
     * 3. 玩家身上有该效果, 但等级高于被给予值 -> 不做处理
     * 4. 玩家身上有该效果, 但等级等于被给予值 -> 清除原效果重新施加
     *
     * @param player 玩家
     * @param effectType 玩家要给予的Buff
     * @param duration 玩家要给予的Buff的持续时间
     * @param level 玩家要给予的Buff的等级
     **/


    @JvmStatic
    fun givePlayerPotionEffect(
        player: Player,
        effectType: PotionEffectType,
        duration: Int, level: Int
    ) {
        val existingEffect = player.activePotionEffects.find { it.type == effectType }
        if (existingEffect == null) {
            player.addPotionEffect(
                PotionEffect(
                    effectType,
                    duration,
                    level,
                    true
                )
            )
        } else {
            if (existingEffect.amplifier > level) return
            if (existingEffect.amplifier >= level && existingEffect.duration > duration) return
            player.removePotionEffect(effectType)
            player.addPotionEffect(
                PotionEffect(
                    effectType,
                    duration,
                    level,
                    true
                )
            )
        }
    }

    fun flushPlayerItem(@Context player: Player) {
        try {
            val inventory = player.inventory
            inventory.forEachIndexed { index, itemStack ->
                val mmItem = ThePit.getInstance().itemFactory.getItemFromStack(itemStack)
                if (mmItem != null) {
                    inventory.remove(index)
                    inventory.setItem(index, mmItem.toItemStack())
                }
            }
        } catch (ignored: Exception) {
            player.sendMessage("Error")
        }
    }

    @JvmStatic
    fun unScamArtist(player: Player, price: Int): Int {
        // 我的世界反推。
        // 计算经过商场欺诈术前应有的原始价格
        val profile = PlayerProfile.getPlayerProfileByUuid(player.uniqueId)
        val scamArtistData = profile.unlockedPerkMap["ScamArtist"]
        return if (scamArtistData != null && scamArtistData.level > 0) {
            val discountFactor = 1 - 0.05 * scamArtistData.level
            (price / discountFactor).toInt() + 1
        } else {
            price
        }
    }

    @JvmStatic
    fun hasInternalItem(player: Player, internalName: String): Boolean {
        return getInternalItemAmount(player, internalName) > 0
    }

    @JvmStatic
    fun hasEnoughInternalItem(player: Player, internalName: String, count: Int): Boolean {
        return getInternalItemAmount(player, internalName) >= count
    }

    @JvmStatic
    fun getItemUuid(itemStack: ItemStack): String? {
        return ItemUtil.getUUID(itemStack)
    }

    // 获取物品的所有附魔记录
    @JvmStatic
    fun getEnchantrecords(itemStack: ItemStack): List<EnchantmentRecord> {
        val item = ThePit.getInstance().itemFactory.getItemFromStack(itemStack) ?: return listOf()
        return item.enchantmentRecords
    }

    // 获取物品的第一个附魔记录
    @JvmStatic
    fun getFirstEnchantRecord(itemStack: ItemStack): EnchantmentRecord? {
        val item = ThePit.getInstance().itemFactory.getItemFromStack(itemStack) ?: return null
        return item.enchantmentRecords.firstOrNull()
    }

    // 获取物品第一次附魔的时间
    @JvmStatic
    fun getFirstEnchantRecordTime(itemStack: ItemStack): Long? {
        val item = ThePit.getInstance().itemFactory.getItemFromStack(itemStack) ?: return null
        return item.enchantmentRecords.firstOrNull()?.timestamp
    }

    // 获取物品第一个附魔者
    @JvmStatic
    fun getFirstEnchantRecordEnchanter(itemStack: ItemStack): String? {
        val item = ThePit.getInstance().itemFactory.getItemFromStack(itemStack) ?: return null
        return item.enchantmentRecords.firstOrNull()?.enchanter
    }
}
