package net.mizukilab.pit.listener;

import cn.charlotte.pit.ThePit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/*
 * author: APNF
 * date: 2025/9/12
 */

/**
 * 过期物品清理。
 *
 * <p>1.21 适配：原先从 NMS 的 {@code extra.expireTime} 复合标签读 long，改为读取物品
 * 持久化数据容器（PDC）里的 {@code expire_time} 长整型。写入方若仍使用旧 NBT 结构，
 * 需要同步改成 PDC 写入。</p>
 */
public class ExpireListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        checkInventory(event.getWhoClicked().getInventory());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        checkInventory(event.getWhoClicked().getInventory());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkInventory(event.getPlayer().getInventory());
        checkInventory(event.getPlayer().getEnderChest());
    }

    @EventHandler
    public void onPlayerDead(PlayerDeathEvent event) {
        checkInventory(event.getEntity().getInventory());
    }

    private NamespacedKey expireKey() {
        return new NamespacedKey(ThePit.getInstance(), "expire_time");
    }

    private long getExpireTime(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0L;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0L;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Long value = container.get(expireKey(), PersistentDataType.LONG);
        return value == null ? 0L : value;
    }

    private void checkInventory(Inventory inv) {
        if (inv == null) return;

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null) {
                continue;
            }

            long expireTime = getExpireTime(item);
            if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
                inv.setItem(slot, null); // 移除物品
                if (inv.getHolder() instanceof Player player) {
                    player.sendMessage(ChatColor.RED + "你的一件物品已过期并被移除!");
                }
            }
        }
    }
}
