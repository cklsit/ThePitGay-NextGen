package net.mizukilab.pit.util.menu.buttons;

import net.mizukilab.pit.util.callback.Callback;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConfirmationButton extends Button {

    private final boolean confirm;
    private final Callback<Boolean> callback;
    private final boolean closeAfterResponse;
    private int delaySec;

    public ConfirmationButton(boolean confirm, Callback<Boolean> callback, boolean closeAfterResponse, int delaySec) {
        this.confirm = confirm;
        this.callback = callback;
        this.closeAfterResponse = closeAfterResponse;
        this.delaySec = delaySec;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        // 1.21：数据值已废弃，Material.TERRACOTTA 是未染色的陶瓦（红褐色），
        // 必须按颜色改用具体材质，否则确认/取消会显示成同一块。
        Material material;
        if (delaySec <= 0) {
            material = this.confirm ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA;
        } else {
            material = this.confirm ? Material.YELLOW_TERRACOTTA : Material.RED_TERRACOTTA;
        }
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (delaySec <= 0) {
            itemMeta.setDisplayName(this.confirm ? CC.GREEN + "确认" : CC.RED + "取消");
        } else {
            itemMeta.setDisplayName(CC.translate(this.confirm ? CC.GREEN + "你确定吗? &7(" + delaySec + ")" : CC.RED + "取消"));
        }
        itemStack.setItemMeta(itemMeta);
        delaySec--;
        return itemStack;
    }

    @Override
    public void clicked(Player player, int i, ClickType clickType, int hb, ItemStack currentItem) {
        if (delaySec >= 0 && confirm) {
            return;
        }

        if (this.confirm) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 20f, 0.1f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 20f, 0.1F);
        }

        if (this.closeAfterResponse) {
            player.closeInventory();
        }

        this.callback.call(this.confirm);
    }

}
