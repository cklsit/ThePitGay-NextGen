package net.mizukilab.pit.fishing;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.compat.math.MathHelper;
import net.mizukilab.pit.parm.AutoRegister;
import net.mizukilab.pit.util.PublicUtil;
import net.mizukilab.pit.util.item.ItemBuilder;
import net.mizukilab.pit.util.random.RandomUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

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
            event.getCaught().remove();

            Location location = event.getCaught().getLocation();

            ItemStack itemStack = this.generateItem(event.getPlayer(), location);
            org.bukkit.entity.Item item = this.generateEntity(event.getPlayer(), location, itemStack);
            item.setMetadata("gold", new FixedMetadataValue(ThePit.getInstance(), RandomUtil.random.nextInt(5) + 5));

        } else if (event.getState() == PlayerFishEvent.State.FISHING) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        }
    }


    private org.bukkit.entity.Item generateEntity(Player player, Location dropLocation, ItemStack item) {
        // 1.21：直接生成 Bukkit 掉落物，速度按原公式换算
        org.bukkit.entity.Item entityItem = dropLocation.getWorld().dropItem(dropLocation, item);
        entityItem.setPickupDelay(10);

        double d5 = player.getLocation().getX() - dropLocation.getX();
        double d6 = player.getLocation().getY() - dropLocation.getY();
        double d7 = player.getLocation().getZ() - dropLocation.getZ();
        double d8 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
        double d9 = 0.1D;

        org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(
                d5 * d9,
                d6 * d9 + Math.sqrt(d8) * 0.08D,
                d7 * d9);
        entityItem.setVelocity(velocity);

        return entityItem;
    }

    ItemBuilder build = new ItemBuilder(Material.GOLD_INGOT);

    private ItemStack generateItem(Player player, Location location) {

        return build
                .build();
    }

}
