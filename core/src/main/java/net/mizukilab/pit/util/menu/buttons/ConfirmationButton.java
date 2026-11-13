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

    /**
     * 倒计时由确认键独占：取消键始终保持红色且不参与倒计时显示。
     */
    private int delaySec;

    public ConfirmationButton(boolean confirm, Callback<Boolean> callback, boolean closeAfterResponse, int delaySec) {
        this.confirm = confirm;
        this.callback = callback;
        this.closeAfterResponse = closeAfterResponse;
        this.delaySec = delaySec;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        // 取消键固定红色；确认键倒计时期间为黄色，倒计时结束后变绿色
        Material material;
        if (!confirm) {
            material = Material.RED_TERRACOTTA;
        } else if (delaySec <= 0) {
            material = Material.LIME_TERRACOTTA;
        } else {
            material = Material.YELLOW_TERRACOTTA;
        }

        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();

        String name;
        if (!confirm) {
            name = CC.RED + "取消";
        } else if (delaySec <= 0) {
            name = CC.GREEN + "确认";
        } else {
            name = CC.translate(CC.GREEN + "你确定吗? &7(" + delaySec + ")");
        }
        itemMeta.setDisplayName(name);
        itemStack.setItemMeta(itemMeta);

        // 只有确认键需要倒计时，取消键的颜色与文字保持不变
        if (confirm && delaySec > 0) {
            delaySec--;
        }
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
