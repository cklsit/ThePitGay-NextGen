package net.mizukilab.pit.menu.admin.item.button;

import net.mizukilab.pit.util.item.ItemUtil;
import net.mizukilab.pit.util.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/28 18:56
 */
public class PitItemButton extends Button {

    private final Supplier<ItemStack> itemSupplier;

    public PitItemButton(ItemStack itemStack) {
        this(() -> itemStack);
    }

    /**
     * 允许延迟生成按钮物品：管理菜单里的部分物品构造代价高，打开时才生成。
     */
    public PitItemButton(Supplier<ItemStack> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        ItemStack itemStack = this.itemSupplier.get();
        return itemStack == null ? null : itemStack.clone();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        ItemStack buttonItem = getButtonItem(player);
        if (buttonItem == null) return;
        if (clickType.isRightClick()) {
            player.sendMessage("这个物品的NBTName为: " + ItemUtil.getInternalName(buttonItem));
            return;
        }
        player.getInventory().addItem(buttonItem);
    }
}
