package cn.irina.thepitaddon.merchant

import cn.irina.thepitaddon.command.player.buyItems.BuyUtil
import net.mizukilab.pit.util.chat.CC
import net.mizukilab.pit.util.item.ItemBuilder
import net.mizukilab.pit.util.menu.Button
import net.mizukilab.pit.util.menu.Menu
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * 时空商人出售的物品定义。
 */
object MerchantItems {

    private val eggBaseLore = listOf("&7死亡后消失", "")

    fun speedEgg(): ItemStack = ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
        .name("&b速度蛋")
        .lore(eggBaseLore + listOf("&7使用后获得 &b速度 III &f(00:08)", "&7(30秒冷却)"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("egg")
        .durability(94)
        .build()

    fun resistanceEgg(): ItemStack = ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
        .name("&3抗性蛋")
        .lore(eggBaseLore + listOf("&7使用后获得 &3抗性提升 II &f(00:08)", "&7(30秒冷却)"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("egg")
        .durability(67)
        .build()

    fun healthBoostEgg(): ItemStack = ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
        .name("&c生命提升蛋")
        .lore(eggBaseLore + listOf("&7使用后获得 &c2❤ 额外生命值 &f(00:30)", "&7(90秒冷却) (不可叠加)"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("egg")
        .durability(96)
        .build()

    fun absorptionEgg(): ItemStack = ItemBuilder(Material.ZOMBIE_SPAWN_EGG)
        .name("&6生命吸收蛋")
        .lore(eggBaseLore + listOf("&7使用后获得 &68❤ 生命吸收 &f(00:30)", "&7(90秒冷却) (不可叠加)"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("egg")
        .durability(62)
        .build()

    fun exDiamondSword(): ItemStack = ItemBuilder(Material.DIAMOND_SWORD)
        .shiny()
        .name("&c强化钻石剑")
        .lore(listOf("&7从时空商人处获得", "", "&7死亡后消失", "", "&9攻击伤害 +8.25", "&9无法破坏"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("shopItem")
        .itemDamage(7.0)
        .enchantment(Enchantment.SHARPNESS, 1)
        .buildWithUnbreakable()

    fun ironHelmet(): ItemStack = ItemBuilder(Material.IRON_HELMET)
        .name("&f铁头盔")
        .lore(listOf("&6从时空商人处获得"))
        .removeOnJoin(false)
        .deathDrop(true)
        .canTrade(true)
        .canSaveToEnderChest(true)
        .internalName("shopItem")
        .buildWithUnbreakable()

    fun physicalCoin(i: Int): ItemStack {
        val name: String = when (i) {
            1 -> "&6一千"
            2 -> "&6一万"
            3 -> "&6十万"
            4 -> "&6一百万"
            5 -> "&6一千万"
            else -> "&c非法的"
        }
        return ItemBuilder(Material.GOLD_INGOT)
            .name("$name" + "硬币")
            .lore(listOf("&7实体货币", "", "&7在神话天坑内广泛流通", "&7可用于兑换购买一些物品"))
            .removeOnJoin(false)
            .deathDrop(false)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .internalName("coin_$i")
            .build()
    }
}

/**
 * 购买按钮：展示物品 + 价格，点击后扣款发货。
 */
class MerchantButton(
    private val itemSupplier: () -> ItemStack,
    private val price: Int,
    private val displayName: String
) : Button() {

    override fun getButtonItem(player: Player): ItemStack {
        val base = itemSupplier()
        val meta = base.itemMeta ?: return base
        val lore = meta.lore?.toMutableList() ?: mutableListOf()
        lore.add("")
        lore.add("&e价格: &6${price} 硬币")
        lore.add("&e点击购买")
        meta.lore = lore.map { CC.translate(it) }
        base.itemMeta = meta
        return base
    }

    override fun clicked(player: Player, slot: Int, clickType: ClickType, hotbarButton: Int, currentItem: ItemStack) {
        BuyUtil.giveItem(player, itemSupplier(), price)
    }
}

/**
 * 时空商人主菜单。
 */
class SpacetimeMerchantMenu : Menu() {

    override fun getTitle(player: Player): String {
        return "时空商人"
    }

    override fun getButtons(player: Player): MutableMap<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        buttons[0] = MerchantButton({ MerchantItems.speedEgg() }, 1000, "速度蛋")
        buttons[1] = MerchantButton({ MerchantItems.resistanceEgg() }, 1000, "抗性蛋")
        buttons[2] = MerchantButton({ MerchantItems.healthBoostEgg() }, 2000, "生命提升蛋")
        buttons[3] = MerchantButton({ MerchantItems.absorptionEgg() }, 2000, "生命吸收蛋")
        buttons[4] = MerchantButton({ MerchantItems.exDiamondSword() }, 650, "强化钻石剑")
        buttons[5] = MerchantButton({ MerchantItems.ironHelmet() }, 100, "铁头盔")
        buttons[8] = object : Button() {
            override fun getButtonItem(player: Player): ItemStack {
                return ItemBuilder(Material.GOLD_INGOT)
                    .name("&6实体货币")
                    .lore(listOf("&7点击兑换实体硬币", "&7(1k / 1w / 10w / 100w / 1kw)"))
                    .build()
            }

            override fun clicked(player: Player, slot: Int, clickType: ClickType, hotbarButton: Int, currentItem: ItemStack) {
                PhysicalCoinMenu().openMenu(player)
            }
        }
        return buttons
    }
}

/**
 * 实体货币兑换子菜单。
 */
class PhysicalCoinMenu : Menu() {

    override fun getTitle(player: Player): String {
        return "实体货币兑换"
    }

    override fun getButtons(player: Player): MutableMap<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()
        buttons[2] = MerchantButton({ MerchantItems.physicalCoin(1) }, 1000, "一千硬币")
        buttons[3] = MerchantButton({ MerchantItems.physicalCoin(2) }, 10000, "一万硬币")
        buttons[4] = MerchantButton({ MerchantItems.physicalCoin(3) }, 100000, "十万硬币")
        buttons[5] = MerchantButton({ MerchantItems.physicalCoin(4) }, 1000000, "一百万硬币")
        buttons[6] = MerchantButton({ MerchantItems.physicalCoin(5) }, 10000000, "一千万硬币")
        return buttons
    }
}
