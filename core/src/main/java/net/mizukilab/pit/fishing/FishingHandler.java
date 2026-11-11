package net.mizukilab.pit.fishing;

import cn.charlotte.pit.ThePit;
import net.mizukilab.pit.parm.AutoRegister;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.random.RandomUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/6 20:12
 */
@AutoRegister
public class FishingHandler implements Listener {

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            event.setExpToDrop(0);
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }

            Location location = event.getCaught() == null
                    ? event.getPlayer().getLocation() : event.getCaught().getLocation();

            ItemStack itemStack = this.generateItem(event.getPlayer(), location);
            Item item = this.generateEntity(event.getPlayer(), location, itemStack);
            item.setMetadata("gold", new FixedMetadataValue(ThePit.getInstance(), RandomUtil.random.nextInt(5) + 5));
        }
    }

    /**
     * 1.21 适配：原先手写 {@code EntityItem} 的 NMS 坐标/速度，改为 Bukkit 掉落物实体 + 速度向量。
     */
    private Item generateEntity(Player player, Location dropLocation, ItemStack item) {
        Item entityItem = dropLocation.getWorld().dropItem(dropLocation, item, entity -> entity.setPersistent(false));

        double dx = player.getLocation().getX() - dropLocation.getX();
        double dy = player.getLocation().getY() - dropLocation.getY();
        double dz = player.getLocation().getZ() - dropLocation.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double factor = 0.1D;

        entityItem.setVelocity(new Vector(
                dx * factor,
                dy * factor + Math.sqrt(distance) * 0.08D,
                dz * factor
        ));
        return entityItem;
    }

    ItemBuilder build = new ItemBuilder(Material.GOLD_INGOT);

    private ItemStack generateItem(Player player, Location location) {
        return build.build();
    }
}
