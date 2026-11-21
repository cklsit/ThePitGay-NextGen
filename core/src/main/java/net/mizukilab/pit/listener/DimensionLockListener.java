package net.mizukilab.pit.listener;

import cn.charlotte.pit.ThePit;
import net.mizukilab.pit.util.chat.CC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 关闭下界与末地：竞技场只使用主世界。
 * <p>
 * 1. 取消所有下界/末地传送门传送；
 * 2. 启动后卸载已加载的下界与末地世界（不再生成、不再保留怪物与区块）。
 * <p>
 * 服务器侧的 allow-nether / allow-end 由部署配置保证，这里兜底运行期行为。
 */
public class DimensionLockListener implements Listener {

    private static final String[] BLOCKED_SUFFIXES = {"_nether", "_the_end"};

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_PORTAL
                || cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (player != null) {
                player.sendMessage(CC.translate("&c本服务器已关闭下界与末地传送门"));
            }
        }
    }

    /** 兜底：任何插件/指令尝试把玩家传送到下界或末地时阻止。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        if (isBlockedName(event.getTo().getWorld().getName())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.translate("&c本服务器已关闭下界与末地"));
        }
    }

    /** 卸载已存在的下界/末地世界，避免它们继续被访问。 */
    public static int unloadBlockedWorlds() {
        int unloaded = 0;
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            if (!isBlockedName(name)) {
                continue;
            }
            try {
                Bukkit.unloadWorld(world, true);
                unloaded++;
            } catch (Throwable failure) {
                ThePit.getInstance().getLogger().warning("[Dimension] 无法卸载世界 " + name + ": " + failure);
            }
        }
        return unloaded;
    }

    public static boolean isBlockedName(String name) {
        if (name == null) {
            return false;
        }
        for (String suffix : BLOCKED_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
