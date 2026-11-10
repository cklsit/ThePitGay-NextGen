package net.mizukilab.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PerkData;
import cn.charlotte.pit.events.EventFactory;
import net.mizukilab.pit.parm.AutoRegister;
import net.mizukilab.pit.util.chat.CC;
import net.mizukilab.pit.util.cooldown.Cooldown;
import net.mizukilab.pit.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/7 12:45
 */
@AutoRegister
public class EnderChestListener implements Listener {

    private final Map<UUID, Location> locationCache = new HashMap<>();
    private final Map<UUID, Cooldown> openCooldown = new HashMap<>();
    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        UUID uniqueId = e.getPlayer().getUniqueId();
        locationCache.remove(uniqueId);
        openCooldown.remove(uniqueId);
    }
    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.getView().title()).equals("末影箱")) {
            if (event.getClick() == ClickType.NUMBER_KEY || event.getAction().name().contains("HOTBAR")) {
                event.setCancelled(true);
                return;
            }
            if ("not_unlock_slot".equals(ItemUtil.getInternalName(event.getCurrentItem()))) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
                if (event.getCurrentItem() != null && !event.getCurrentItem().getType().equals(Material.AIR) && !ItemUtil.canSaveEnderChest(event.getCurrentItem())) {
                    event.setCancelled(true);
                }
            } else {
                if (!event.getCursor().getType().equals(Material.AIR) && !ItemUtil.canSaveEnderChest(event.getCursor())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.getView().title()).equals("末影箱")) {
            final Location location = locationCache.remove(event.getPlayer().getUniqueId());

            if (location != null) {
                ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0.6F);

                // 1.21：箱子开合动画由客户端根据容器包播放，无需额外发包
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().contains("末影箱")) {
            player.closeInventory();
            player.sendMessage(CC.translate("&c请尝试再次打开末影箱."));
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (block != null && block.getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);

            if (!player.hasPermission("pit.admin")) {
                Cooldown cooldown = this.openCooldown.get(player.getUniqueId());
                if (cooldown != null && !cooldown.hasExpired()) {
                    player.sendMessage(CC.translate("&c请稍后再打开末影箱!"));
                    return;
                }
                this.openCooldown.put(player.getUniqueId(), new Cooldown(3, TimeUnit.SECONDS));
            }

            final EventFactory factory = ThePit.getInstance().getEventFactory();
            if ("spire".equals(factory.getActiveEpicEventName())) {
                player.sendMessage(CC.translate("&c末影箱当前被禁用了!"));
                return;
            }

            final Location location = event.getClickedBlock().getLocation();
            final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

            if (profile.isInArena()) {
                return;
            }

            PerkData data = profile.getUnlockedPerkMap().get("extra_enderchest");
            if (data != null) {
                profile.setEnderChestRow(3 + data.getLevel());
            }

            profile.getEnderChest().openEnderChest(player);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);

            this.locationCache.put(player.getUniqueId(), location);

            // 1.21：箱子开合动画由客户端根据容器包播放，无需额外发包
        }
    }

}
