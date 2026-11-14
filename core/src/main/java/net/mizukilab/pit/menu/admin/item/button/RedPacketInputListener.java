package net.mizukilab.pit.menu.admin.item.button;

import cn.charlotte.pit.ThePit;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mizukilab.pit.util.chat.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 红包金额输入：用聊天栏输入替代虚拟告示牌。
 * 1.21.8 下虚拟告示牌编辑器会让客户端闪退，因此改为聊天输入。
 */
public class RedPacketInputListener implements Listener {

    private static final long TIMEOUT_MS = 60_000L;
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private record Pending(long expireAt, Consumer<Integer> callback) {
    }

    /** 让玩家在聊天栏输入金额，成功后回到主线程执行回调。 */
    public static void request(Player player, Consumer<Integer> callback) {
        PENDING.put(player.getUniqueId(), new Pending(System.currentTimeMillis() + TIMEOUT_MS, callback));
        player.sendMessage(CC.translate("&c&l红包 &7请在聊天栏输入金额（&e1-1000000&7，输入 &c取消 &7放弃）"));
    }

    public static boolean hasPending(Player player) {
        return PENDING.containsKey(player.getUniqueId());
    }

    /** 处理一次输入；返回 true 表示该消息属于红包输入并被消费。 */
    public static boolean handleInput(Player player, String input) {
        Pending pending = PENDING.remove(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() > pending.expireAt()) {
            player.sendMessage(CC.translate("&c红包 &7输入超时"));
            return true;
        }
        String text = input == null ? "" : input.trim();
        if (text.equalsIgnoreCase("取消")) {
            player.sendMessage(CC.translate("&c红包 &7已取消"));
            return true;
        }
        int amount = parseAmount(text);
        if (amount < 0) {
            player.sendMessage(CC.translate("&c红包 &7请输入有效数字"));
            return true;
        }
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            if (player.isOnline()) {
                pending.callback().accept(amount);
            }
        });
        return true;
    }

    /** 供测试与命令复用：解析金额，非法返回 -1。 */
    public static int parseAmount(String input) {
        if (input == null) return -1;
        String text = input.trim();
        if (text.isEmpty()) return -1;
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? value : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!hasPending(player)) {
            return;
        }
        event.setCancelled(true);
        handleInput(player, PlainTextComponentSerializer.plainText().serialize(event.message()));
    }
}
