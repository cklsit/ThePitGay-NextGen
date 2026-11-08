package cn.irina.thepitaddon.manager

import cn.charlotte.pit.ThePit
import cn.irina.thepitaddon.Main
import cn.irina.thepitaddon.manager.PitManager.getFirstEnchantRecordEnchanter
import cn.irina.thepitaddon.manager.PitManager.getFirstEnchantRecordTime
import cn.irina.thepitaddon.manager.PitManager.getInternalName
import cn.irina.thepitaddon.manager.PitManager.getItemUuid
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

class FilterManager(private val plugin: Main) {

    /**
     * @Author ShanguanLinG
     * @Date 2025/10/08 0:00
     */

    private var filterConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "filter.yml"))

    fun reloadFilterConfig() {
        filterConfig = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "filter.yml"))
    }

    /**
     * 对玩家应用所有启用的过滤器规则
     * @param player 需要应用过滤器的玩家
     */
    fun applyFilters(player: Player) {
        for (filterKey in filterConfig.getKeys(false)) {
            val filterSection = filterConfig.getConfigurationSection(filterKey) ?: continue
            if (!isFilterEnabled(filterSection)) {
                continue
            }
            checkEquipmentAndFilterItem(player, filterSection)
            checkInventoryAndFilterItem(player, filterSection)
        }
    }

    /**
     * 检查过滤器是否启用
     * @param filterSection 过滤器配置部分
     * @return 如果启用返回true，否则返回false
     */
    private fun isFilterEnabled(filterSection: ConfigurationSection): Boolean {
        return filterSection.getBoolean("enabled", false)
    }

    /**
     * 检查并处理玩家物品
     * @param player 玩家对象
     * @param filterSection 过滤器配置部分
     */
    private fun checkInventoryAndFilterItem(player: Player, filterSection: ConfigurationSection) {
        val inventory = player.inventory
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (item.type == Material.AIR) continue
            if (matchesFilter(item, filterSection)) {
                doFilterItem(player, i, item, false)
            }
        }
    }

    /**
     * 检查并处理玩家装备
     * @param player 玩家对象
     * @param filterSection 过滤器配置部分
     */
    private fun checkEquipmentAndFilterItem(player: Player, filterSection: ConfigurationSection) {
        val equipment = player.inventory.armorContents
        for (i in equipment.indices) {
            val item = equipment[i] ?: continue
            if (item.type == Material.AIR) continue
            if (matchesFilter(item, filterSection)) {
                doFilterItem(player, i, item, true)
            }
        }
    }

    /**
     * 检查物品是否匹配过滤器规则
     * @param item 需要检查的物品
     * @param filterSection 过滤器配置部分
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesFilter(item: ItemStack, filterSection: ConfigurationSection): Boolean {
        if (!matchesUuidBlacklist(item, filterSection)) return false
        if (!matchesEnchantmentRecords(item, filterSection)) return false
        if (!matchesInterNalName(item, filterSection)) return false
        return true
    }

    /**
     * 检查物品的内部名是否在internal黑名单中
     * @param item 需要检查的物品
     * @param filterSection 过滤器配置部分
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesInterNalName(item: ItemStack, filterSection: ConfigurationSection): Boolean {
        val internalBlacklist = filterSection.getStringList("internal-blacklist")
        if (internalBlacklist.isEmpty() || (internalBlacklist.size == 1 && internalBlacklist[0].isBlank())) return true
        val internalName = getInternalName(item)
        return internalBlacklist.contains(internalName)
    }

    /**
     * 检查物品是否在UUID黑名单中
     * @param item 需要检查的物品
     * @param filterSection 过滤器配置部分
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesUuidBlacklist(item: ItemStack, filterSection: ConfigurationSection): Boolean {
        val uuidBlacklist = filterSection.getStringList("uuid-blacklist")
        if (uuidBlacklist.isEmpty() || (uuidBlacklist.size == 1 && uuidBlacklist[0].isBlank())) return true
        val itemUuid = getItemUuid(item)
        return itemUuid != null && uuidBlacklist.contains(itemUuid)
    }

    /**
     * 检查物品的附魔记录是否匹配
     * @param item 需要检查的物品
     * @param filterSection 过滤器配置部分
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesEnchantmentRecords(item: ItemStack, filterSection: ConfigurationSection): Boolean {
        val enchantmentRecordsSection =
            filterSection.getConfigurationSection("enchantment-records-blacklist") ?: return true
        return matchesFirstEnchanter(item, enchantmentRecordsSection) && matchesFirstEnchantmentTime(
            item,
            enchantmentRecordsSection
        )
    }

    /**
     * 检查物品的首次附魔者是否匹配
     * @param item 需要检查的物品
     * @param enchantmentRecordsSection 附魔记录配置部分
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesFirstEnchanter(item: ItemStack, enchantmentRecordsSection: ConfigurationSection): Boolean {
        val firstEnchanters = enchantmentRecordsSection.getStringList("first-enchanter")
        if (firstEnchanters.isEmpty() || (firstEnchanters.size == 1 && firstEnchanters[0].isBlank())) return true
        val firstEnchanter = getFirstEnchantRecordEnchanter(item)
        return firstEnchanter != null && firstEnchanters.contains(firstEnchanter)
    }

    /**
     * 检查物品的首次附魔时间是否匹配
     * @param item 需要检查的物品
     * @param enchantmentRecordsSection 附魔记录配置
     * @return 如果匹配返回true，否则返回false
     */
    private fun matchesFirstEnchantmentTime(item: ItemStack, enchantmentRecordsSection: ConfigurationSection): Boolean {
        val firstEnchantmentTimes = enchantmentRecordsSection.getStringList("first-enchantment-time")
        if (firstEnchantmentTimes.isEmpty() || (firstEnchantmentTimes.size == 1 && firstEnchantmentTimes[0].isBlank())) return true
        val firstEnchantmentTime = getFirstEnchantRecordTime(item)
        if (firstEnchantmentTime != null) {
            val timeMatch =
                firstEnchantmentTimes.any { time -> if (time.isBlank()) false else firstEnchantmentTime == time.toLong() }
            return timeMatch
        }
        return false
    }

    /**
     * 处理匹配过滤器的物品
     * @param player 玩家对象
     * @param slot 物品物品栏
     * @param item 需要处理的物品
     * @param isArmor 是否为装备
     */
    private fun doFilterItem(
        player: Player,
        slot: Int,
        item: ItemStack,
        isArmor: Boolean
    ) {
        val defUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val build = ItemBuilder(item)
            .changeNbt("ench", "")
            .changeNbt("saved", false)
            .uuid(defUUID)
            .build()
        val mythicItem = ThePit.getInstance().itemFactory.getItemFromStack(build)
        if (mythicItem == null) {
            val slotDisplay = if (isArmor) "装备栏" else "第${slot + 1}格"
            player.sendMessage(CC.translate("&c检测到你背包的${slotDisplay}物品被管理员标记为非法物品, 请及时上交管理员."))
            return
        }
        val newItem = mythicItem.toItemStack()

        if (isArmor) {
            // 处理装备栏物品
            val armorContents = player.inventory.armorContents
            armorContents[slot] = newItem
            player.inventory.armorContents = armorContents
        } else {
            // 处理普通物品栏
            player.inventory.setItem(slot, newItem)
        }

        PitManager.flushPlayerItem(player)
        val slotDisplay = if (isArmor) "装备栏" else "第${slot + 1}格"
        player.sendMessage(CC.translate("&c检测到你背包的${slotDisplay}物品被管理员标记为非法物品, 已自动为您处理为白板神话物品."))
    }

}