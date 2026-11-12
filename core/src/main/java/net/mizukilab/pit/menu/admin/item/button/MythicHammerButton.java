package net.mizukilab.pit.menu.admin.item.button;

import net.mizukilab.pit.item.type.mythic.MythicHammerItem;
import net.mizukilab.pit.menu.admin.item.HammerEnchantMenu;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * 神话之锤入口：左键获得一把空白锤子，右键打开锤子附魔选择菜单。
 */
public class MythicHammerButton extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        ItemStack base = new MythicHammerItem().toItemStack();
        return new ItemBuilder(base)
                .lore("", "&e左键 &7获得一把空白神话之锤", "&e右键 &7打开锤子附魔菜单")
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (clickType.isRightClick()) {
            new HammerEnchantMenu().openMenu(player);
            return;
        }
        if (clickType.isLeftClick()) {
            player.getInventory().addItem(new MythicHammerItem().toItemStack());
            playSuccess(player);
        }
    }
}
