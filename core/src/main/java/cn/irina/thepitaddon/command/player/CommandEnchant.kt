package cn.irina.thepitaddon.command.player

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.data.sub.EnchantmentRecord
import cn.irina.thepitaddon.Main
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.mizukilab.pit.enchantment.param.item.ArmorOnly
import net.mizukilab.pit.enchantment.param.item.BowOnly
import net.mizukilab.pit.enchantment.param.item.WeaponOnly
import net.mizukilab.pit.enchantment.rarity.EnchantmentRarity
import net.mizukilab.pit.item.AbstractPitItem
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import net.mizukilab.pit.util.item.ItemUtil
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/*
 * @Author Irina, ShanguanLinG
 * @Date 2025/6/19
 */

@Command(name = "enchantToTarget")
class CommandEnchant {
    val prefix = Main.instance.PREFIX

    @Execute
    fun onCommand(
        @Context sender: CommandSender,
        @Arg enchantName: String,
        @Arg level: Int,
        @Arg player: Player
    ) {
        // 检查手持物品
        if (player.itemInHand == null || player.itemInHand.type == Material.AIR) {
            player.sendMessage(CC.translate("$prefix&c手持物不得为空!"))
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }

        // 检查是否已用指令附魔
        if (hasCommandEnchanted(player.itemInHand)) {
            player.sendMessage(CC.translate("$prefix&c此物品已被指令附魔, 或不是一件有效的神话物品!"))
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }

        // 检查是否含有足够的神话凝聚体
        val amazingGemAmount = getMythicCondenserCount(player, enchantName, level)
        if (amazingGemAmount <= 0) {
            player.sendMessage(CC.translate("$prefix&c你没有足够的神话凝聚体!"))
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }
        val oldItem = player.itemInHand
        // 尝试附魔
        player.itemInHand = onPlayerEnchant(player, player.itemInHand, enchantName, level)
        // 检查是否附魔成功, 成功则每次附魔消耗一个凝聚体
        if (hasSuccessfulEnchanted(oldItem, player.itemInHand)) {
            takeMythicCondenser(player, enchantName, level, 1)
        } else {
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }
        player.sendMessage(CC.translate("$prefix&aSUCCESS"))
    }

    private fun hasSuccessfulEnchanted(oldItem: ItemStack, newItem: ItemStack): Boolean {
        return !(oldItem.type == newItem.type
                && oldItem.itemMeta == newItem.itemMeta)
    }

    private val mythicCondenser = "mythic_condenser"
    private fun isMythicCondenser(i: ItemStack): Boolean {
        return i.type != Material.AIR
                && i.type == Material.NETHER_STAR
                && mythicCondenser == ItemUtil.getInternalName(i)
    }

    private fun getMythicCondenserCount(p: Player, e: String, l: Int): Int {
        var amount = 0
        for (item in p.inventory) {
            if (item == null || item.type == Material.AIR) continue
            if (!isMythicCondenser(item)) continue
            val enchant = ItemUtil.getItemStringData(item, "enchant")
            if (e != enchant) continue
            val level = ItemUtil.getItemIntData(item, "level")
            if (l != level) continue
            amount += item.amount
        }

        return amount
    }

    private fun takeMythicCondenser(p: Player, e: String, l: Int, count: Int) {
        var remaining = count
        val inventory = p.inventory

        for (slot in 0 until inventory.size) {
            if (remaining <= 0) break
            val item = inventory.getItem(slot) ?: continue

            if (!isMythicCondenser(item)) continue

            if (e != ItemUtil.getItemStringData(item, "enchant")) continue
            if (l != ItemUtil.getItemIntData(item, "level")) continue

            val takeAmount = minOf(item.amount, remaining)

            if (item.amount > takeAmount) {
                item.amount -= takeAmount
                inventory.setItem(slot, item)
            } else {
                inventory.setItem(slot, null)
            }

            remaining -= takeAmount
        }
        p.updateInventory()
    }

//    private fun onEnchant(item: ItemStack, name: String, level: Int): ItemStack? {
//        val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(item)
//        val enchant = ThePit.getInstance().enchantmentFactor.enchantmentMap[name] ?: return item
//        val oldMap = pitItem.enchantments.apply { put(enchant, level) }
//        pitItem.enchantments = oldMap
//        return pitItem.toItemStack()
//    }

    private fun hasCommandEnchanted(i: ItemStack): Boolean {
        val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(i) ?: return true
        for (record in pitItem.enchantmentRecords) {
            if (!record.description.contains("EnchantCommand")) continue
            return true
        }
        return false
    }

    private fun onPlayerEnchant(player: Player, item: ItemStack, enchantName: String, level: Int): ItemStack {
        if (ItemUtil.getItemIntData(item, "tier") == null) {
            player.sendMessage(CC.translate("$prefix&c此物品没有阶数!"))
            return item
        }

        if (ItemUtil.getItemIntData(item, "tier") >= 3) {
            player.sendMessage(CC.translate("$prefix&c此物品无法继续附魔, 因为阶数大于或等于 3"))
            return item
        }

//        // 不必要的逻辑
//        if (hasCommandEnchanted(item)) {
//            player.sendMessage(CC.translate("$prefix&c此物品已被指令附魔!"))
//            return item
//        }

        val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(item)

        if (pitItem.enchantments.size >= 3) {
            player.sendMessage(CC.translate("$prefix&c此物品无法继续附魔, 因为附魔数量大于或等于 3"))
            return item
        }

        var tier = ItemUtil.getItemIntData(item, "tier")
        if (tier <= 0) tier = 1

        var maxLive = ItemUtil.getItemIntData(item, "maxLive") + 10
        if (maxLive <= 0) maxLive = 10

        var live = ItemUtil.getItemIntData(item, "live") + 10
        if (live <= 0) live = 10

        val enchant = ThePit.getInstance().enchantmentFactor.enchantmentMap[enchantName] ?: return item
        val typesMap = ConcurrentHashMap<String, Boolean>()

        enchant.apply {
            typesMap["bow"] = this::class.java.isAnnotationPresent(BowOnly::class.java)
            typesMap["weapon"] = this::class.java.isAnnotationPresent(WeaponOnly::class.java)
            typesMap["armor"] = this::class.java.isAnnotationPresent(ArmorOnly::class.java)
        }

        when (ItemUtil.getInternalName(item).uppercase()) {
            "MYTHIC_SWORD" -> {
                if (typesMap["weapon"] == false) {
                    player.sendMessage(CC.translate("$prefix&c此附魔不能附魔在 &e神话之剑 &c上!"))
                    return item
                }
            }

            "MYTHIC_BOW" -> {
                if (typesMap["bow"] == false) {
                    player.sendMessage(CC.translate("$prefix&c此附魔不能附魔在 &e神话之弓 &c上!"))
                    return item
                }
            }

            "MYTHIC_LEGGINGS" -> {
                if (typesMap["armor"] == false) {
                    player.sendMessage(CC.translate("$prefix&c此附魔不能附魔在 &e神话之甲 &c上!"))
                    return item
                }
            }

            else -> {
                player.sendMessage(CC.translate("$prefix&c请手持对应可被附魔的物品!"))
                return item
            }
        }

        val oldMap = pitItem.enchantments.apply {
            val hasEnchantLevel = ThePit.getApi().getItemEnchantLevel(item, enchant.nbtName)
            if (hasEnchantLevel >= 1) {
                put(enchant, if (hasEnchantLevel + level > 3) 3 else hasEnchantLevel + level)
            } else {
                put(enchant, if (level > 3) 3 else level)
            }
        }

        pitItem.enchantments = oldMap
        pitItem.enchantmentRecords += EnchantmentRecord(
            player.displayName,
            "EnchantCommand",
            System.currentTimeMillis()
        )

        val resultItem = ItemBuilder(pitItem.toItemStack())
            .changeNbt("maxLive", maxLive)
            .changeNbt("live", live)
            .changeNbt("tier", tier)
            .buildWithUnbreakable()

        return resultItem
    }
}

@Command(name = "unEnchant")
class unEnchant {
    val prefix = Main.instance.PREFIX

    @Execute
    fun onCommand(@Context sender: CommandSender, @Arg enchantName: String, @Arg level: Int, @Arg player: Player) {
        val enchantObject = ThePit.getInstance().enchantmentFactor.enchantmentMap[enchantName]
        if (hasCommandUnEnchanted(player.itemInHand)) {
            player.sendMessage(CC.translate("$prefix&c此物品已被指令祛魔, 或不是一件有效的神话物品!"))
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }

        if (enchantObject == null) {
            sender.sendMessage(CC.translate("$prefix&c未知的附魔!"))
            return
        }

        if (level !in 1..3) {
            sender.sendMessage(CC.translate("$prefix&c等级范围为 &f\"1 ~ 3\""))
            return
        }

        if (player.itemInHand == null || player.itemInHand.type == Material.AIR) {
            sender.sendMessage(CC.translate("$prefix&c非法的物品!"))
            return
        }
        var handItem: ItemStack? = player.itemInHand
        val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(handItem) ?: return
        val enchants = pitItem.enchantments

        if (enchants.isEmpty()) {
            sender.sendMessage(CC.translate("$prefix&f${player.displayName} &c手持物的附魔为空!"))
            return
        }

        if (!enchants.contains(enchantObject)) {
            sender.sendMessage(CC.translate("$prefix&c手持物没有目标附魔"))
            return
        }

        val enchantLevel = enchants[enchantObject] ?: return

        if (enchantLevel > level) {
            sender.sendMessage(CC.translate("$prefix&c附魔等级不得高于逆向逆向神话凝聚体的等级!"))
            return
        }

        if (isExtraordinaryItem(pitItem)) {
            sender.sendMessage(CC.translate("$prefix&c你不能在不凡或不朽的物品上使用逆向神话凝聚体!"))
            return
        }

        if (hasOneRarerEnchantAndOneLimitedEnchant(pitItem)) {
            sender.sendMessage(CC.translate("$prefix&c你不能在不凡或不朽的物品上使用逆向神话凝聚体!"))
            sender.sendMessage(CC.translate("$prefix&c限定附魔也算一个稀有:)"))
            return
        }

        val reverseMythicCondenserAmount = getReverseMythicCondenserCount(player, enchantName, level)

        if (reverseMythicCondenserAmount <= 0) {
            player.sendMessage(CC.translate("$prefix&c你没有足够的逆向神话凝聚体!"))
            player.sendMessage(CC.translate("$prefix&cFAILED!"))
            return
        }

        enchants.remove(enchantObject)
        pitItem.enchantments = enchants
        pitItem.enchantmentRecords += EnchantmentRecord(
            player.displayName,
            "HasUnEnchanted",
            System.currentTimeMillis()
        )

        val pitItemToStack = pitItem.toItemStack()
        val maxLive = ItemUtil.getItemIntData(pitItemToStack, "maxLive")
        val live = ItemUtil.getItemIntData(pitItemToStack, "live")
        val tier = ItemUtil.getItemIntData(pitItemToStack, "tier") - 1
        val resultItem = ItemBuilder(pitItemToStack)
            .changeNbt("maxLive", maxLive)
            .changeNbt("live", live)
            .changeNbt("tier", tier)
            .buildWithUnbreakable()

        player.itemInHand = resultItem
        takeReverseMythicCondenser(player, enchantName, level, 1)
        player.sendMessage(CC.translate("$prefix&aSUCCESS"))
    }

    private fun isExtraordinaryItem(pitItem: AbstractPitItem): Boolean {
        // 是不凡物品(双稀有)
        return getRarerEnchantCount(pitItem) >= 2
    }

    private fun hasOneRarerEnchantAndOneLimitedEnchant(pitItem: AbstractPitItem): Boolean {
        var rarerEnchantCount = getRarerEnchantCount(pitItem)
        var limitedEnchantCount = getLimitedEnchantCount(pitItem)
        return rarerEnchantCount == 1 && limitedEnchantCount == 1
    }

    private fun getLimitedEnchantCount(pitItem: AbstractPitItem): Int {
        var limitedEnchantCount = 0
        for (enchant in pitItem.enchantments.keys) {
            if (enchant.rarity == EnchantmentRarity.OP
                || enchant.rarity == EnchantmentRarity.AUCTION_LIMITED
                || enchant.rarity == EnchantmentRarity.AUCTION_LIMITED_RARE
                || enchant.rarity == EnchantmentRarity.UBER_LIMITED
                || enchant.rarity == EnchantmentRarity.UBER_LIMITED_RARE
            ) {
                limitedEnchantCount++
            }
        }
        return limitedEnchantCount
    }

    private fun getRarerEnchantCount(pitItem: AbstractPitItem): Int {
        var rareEnchantCount = 0
        for (enchant in pitItem.enchantments.keys) {
            if (enchant.rarity == EnchantmentRarity.RARE
                || enchant.rarity == EnchantmentRarity.RAGE_RARE
                || enchant.rarity == EnchantmentRarity.DARK_RARE
                || enchant.rarity == EnchantmentRarity.FISH_RARE
                || enchant.rarity == EnchantmentRarity.SEWER_RARE
                || enchant.rarity == EnchantmentRarity.NOSTALGIA_RARE
            ) {
                rareEnchantCount++
            }
        }
        return rareEnchantCount
    }

    private fun hasCommandUnEnchanted(i: ItemStack): Boolean {
        val pitItem = ThePit.getInstance().itemFactory.getItemFromStack(i) ?: return true
        for (record in pitItem.enchantmentRecords) {
            if (!record.description.contains("HasUnEnchanted")) continue
            return true
        }
        return false
    }

    private fun getReverseMythicCondenserCount(p: Player, e: String, l: Int): Int {
        var amount = 0
        for (item in p.inventory) {
            if (item == null || item.type == Material.AIR) continue
            if (!isReverseMythicCondenser(item)) continue
            val enchant = ItemUtil.getItemStringData(item, "enchant")
            if (e != enchant) continue
            val level = ItemUtil.getItemIntData(item, "level")
            if (l != level) continue
            amount += item.amount
        }
        return amount
    }

    private fun takeReverseMythicCondenser(p: Player, e: String, l: Int, count: Int) {
        var remaining = count
        val inventory = p.inventory
        for (slot in 0 until inventory.size) {
            if (remaining <= 0) break
            val item = inventory.getItem(slot) ?: continue
            if (!isReverseMythicCondenser(item)) continue
            if (e != ItemUtil.getItemStringData(item, "enchant")) continue
            if (l != ItemUtil.getItemIntData(item, "level")) continue
            val takeAmount = minOf(item.amount, remaining)
            if (item.amount > takeAmount) {
                item.amount -= takeAmount
                inventory.setItem(slot, item)
            } else {
                inventory.setItem(slot, null)
            }
            remaining -= takeAmount
        }
        p.updateInventory()
    }

    private fun isReverseMythicCondenser(i: ItemStack): Boolean {
        return i.type != Material.AIR
                && ItemUtil.getInternalName(i).equals("reverse_mythic_condenser", ignoreCase = true)
                && ItemUtil.getItemStringData(i, "enchant") != null
                && ItemUtil.getItemIntData(i, "level") != null
    }
}