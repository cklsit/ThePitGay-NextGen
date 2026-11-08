package cn.irina.thepitaddon

import cn.charlotte.pit.ThePit
import cn.charlotte.pit.api.PitInternalHook
import net.mizukilab.pit.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

class PitItem {
    private val thePit: PitInternalHook = ThePit.getApi()


    val afkFragment: ItemStack by lazy {
        val lore: MutableList<String> = ArrayList()

        lore.add("&e挂机奖励")
        lore.add("&7在灵泉挂机池中获取的碎片")
        lore.add("&7似乎可以兑换一些物品...")

        ItemBuilder(Material.SUGAR)
            .name("&b灵泉&f碎片")
            .lore(lore)
            .internalName("afk_fragment")
            .canTrade(true)
            .canSaveToEnderChest(true)
            .build()
    }

    val funkyFeather: ItemStack by lazy {
        val lore: MutableList<String> = ArrayList()

        lore.add("&e特殊物品")
        lore.add("&7放于物品栏时,可以保护")
        lore.add("&7背包内的神话物品不会在死亡后扣除生命.")
        lore.add("&7&o此物品会在死亡后消耗")

        ItemBuilder(Material.FEATHER)
            .name("&3时髦的羽毛")
            .lore(lore)
            .internalName("funky_feather")
            .canTrade(true)
            .canSaveToEnderChest(true)
            .build()
    }

    val chunkOfVile: ItemStack by lazy {
        ItemBuilder(Material.COAL)
            .name("&5暗聚块")
            .lore(
                "&7死亡后保留",
                "",
                "&c邪术收藏品"
            )
            .canSaveToEnderChest(true)
            .canTrade(true)
            .internalName("chunk_of_vile_item")
            .build();
    }

    val cactus: ItemStack by lazy {
        val lore: MutableList<String> = ArrayList()

        lore.add("&e特殊物品")
        lore.add("&7手持并右键可以从九件未附魔的")
        lore.add("&7随机 &a神&c话&e之&6甲 &7选择其一.")
        lore.add(" ")
        lore.add("&7(部分特殊颜色不可选择)")

        ItemBuilder(Material.CACTUS)
            .name("&a哲学仙人掌")
            .lore(lore)
            .internalName("cactus")
            .canTrade(true)
            .canSaveToEnderChest(true).build()
    }

    val globalAttentionGem: ItemStack by lazy {
        ItemBuilder(Material.DIAMOND)
            .name("&b举世瞩目的宝石")
            .lore(
                "&7死亡时保留",
                "",
                "&7增加附魔物品的一级附魔, 并使神话物品生命后添加 &b♦ &7的字符",
                "&7(普通及特殊附魔除外, 不可超过上限)",
                "&8一件物品只能使用一次",
                "",
                "&e右键使用"
            )
            .internalName("global_attention_gem")
            .shiny()
            .removeOnJoin(false)
            .deathDrop(false)
            .canDrop(false)
            .canTrade(true)
            .enchant(Enchantment.DURABILITY, 1)
            .canSaveToEnderChest(true)
            .build()
    }

    val totallyLegitGem: ItemStack by lazy {
        ItemBuilder(Material.EMERALD)
            .name("&a遵纪守法的宝石")
            .lore(
                "&7死亡时保留",
                "",
                "&7增加附魔物品的一级附魔, 并使神话物品生命后添加 &a♦ &7的字符",
                "&7(稀有及特殊附魔除外, 不可超过上限)",
                "&8一件物品只能使用一次",
                "",
                "&e右键使用"
            )
            .internalName("totally_legit_gem")
            .shiny()
            .removeOnJoin(false)
            .deathDrop(false)
            .canDrop(false)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .build()
    }

    val repairKit: ItemStack by lazy {
        ItemBuilder(Material.SHEARS)
            .name("&d神话工匠包")
            .lore(
                "&e特殊物品",
                "&c无法用于交易",
                "&7手持要修复的神话物品,在背包内右键点击神话工匠包,",
                "&7即可恢复手上的神话物品全部生命.",
                "&7(神话工匠包会在使用后消耗)"
            )
            .internalName("mythic_repair_kit")
            .canSaveToEnderChest(true)
            .canTrade(false)
            .shiny()
            .buildWithUnbreakable()
    }

    val cherry: ItemStack by lazy {
        ItemBuilder(Material.APPLE)
            .internalName("cherry")
            .canDrop(true)
            .canTrade(true)
            .deathDrop(false)
            .canSaveToEnderChest(true)
            .name("&d樱桃")
            .lore(
                "&7死亡时保留"
            )
            .build()
    }

    fun randomColorMythicLegging(): ItemStack {
        return thePit.generateItem("Leggings")
    }
}
