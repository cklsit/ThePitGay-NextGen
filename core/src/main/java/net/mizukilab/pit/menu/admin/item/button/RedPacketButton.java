package net.mizukilab.pit.menu.admin.item.button;

import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/30 22:38
 */
public class RedPacketButton extends Button {

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&c红包")
                .lore("", "&7里面会有什么呢", "")
                .internalName("red_packet")
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        // 1.21.8 客户端在虚拟告示牌编辑器上会崩溃，改为聊天栏输入金额
        player.closeInventory();
        RedPacketInputListener.request(player, money -> {
            if (money <= 0) {
                return;
            }
            player.getInventory().addItem(new ItemBuilder(this.getButtonItem(player))
                    .changeNbt("money", money)
                    .changeNbt("sender", player.getUniqueId().toString())
                    .build());
            player.sendMessage(CC.translate("&c红包 &7已创建，金额 &e" + money + " &7金"));
        });
    }
}
