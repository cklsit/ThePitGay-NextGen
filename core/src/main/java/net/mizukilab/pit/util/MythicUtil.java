package net.mizukilab.pit.util;

import cn.charlotte.pit.ThePit;
import net.mizukilab.pit.item.IMythicItem;
import net.mizukilab.pit.item.factory.ItemFactory;
import org.bukkit.inventory.ItemStack;

public class MythicUtil {

    /**
     * @param itemStack
     * @return 返回物品读取为IMythicItem后的形式, 如此物品不是MythicItem则返回null
     * (此物品需要能被附魔)
     */
    public static IMythicItem getMythicItem(ItemStack itemStack) {//sync
        return ((ItemFactory) ThePit.getInstance().getItemFactory()).getIMythicItemSync(itemStack);
    }



    // Kotlin 无法直接给 Java 公有字段赋值（Lombok 生成的 setter 在 Kotlin 侧不可见），
    // 这里提供显式 setter 供命令层调用。
    public static void setLive(net.mizukilab.pit.item.IMythicItem item, int live) {
        item.live = live;
    }

    public static void setMaxLive(net.mizukilab.pit.item.IMythicItem item, int maxLive) {
        item.maxLive = maxLive;
    }

    public static void setTier(net.mizukilab.pit.item.IMythicItem item, int tier) {
        item.tier = tier;
    }

    public static void setColor(net.mizukilab.pit.item.IMythicItem item, net.mizukilab.pit.item.MythicColor color) {
        item.color = color;
    }

    public static void setUuid(net.mizukilab.pit.item.AbstractPitItem item, java.util.UUID uuid) {
        item.uuid = uuid;
    }
}
