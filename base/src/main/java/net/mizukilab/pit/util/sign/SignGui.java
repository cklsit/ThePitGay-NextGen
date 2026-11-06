package net.mizukilab.pit.util.sign;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟告示牌输入框。
 * <p>
 * 1.21 适配：原实现依赖 ProtocolLib 拦截 {@code UPDATE_SIGN} 并强制转换 CraftPlayer；
 * 现改为 PacketEvents（已内置）：仅对该玩家伪造告示牌方块与文本，下发打开编辑器包，再拦截提交包读取四行文本。
 *
 * @Author: EmptyIrony
 * @Date: 2021/1/19 21:33
 */
public class SignGui {

    protected Map<String, Vector> signLocations = new ConcurrentHashMap<>();
    protected Map<String, SignGUIListener> listeners = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;

    public SignGui(JavaPlugin plugin) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) {
                    return;
                }
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                Vector expected = signLocations.get(player.getName());
                if (expected == null) {
                    return;
                }
                WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
                Vector3i position = wrapper.getBlockPosition();
                if (position.getX() != expected.getBlockX()
                        || position.getY() != expected.getBlockY()
                        || position.getZ() != expected.getBlockZ()) {
                    return;
                }
                signLocations.remove(player.getName());
                SignGUIListener response = listeners.remove(player.getName());
                String[] lines = wrapper.getTextLines();
                if (response != null) {
                    event.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> response.onSignDone(player, lines), 1L);
                }
            }
        });
    }

    public void open(Player player, String[] messages, SignGUIListener response) {
        Location loc = new Location(player.getWorld(), 0, 1, 0);
        // 只对该玩家伪造方块与文本，服务端不产生真实方块
        player.sendBlockChange(loc, Material.OAK_SIGN.createBlockData());
        player.sendSignChange(loc, messages);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerOpenSignEditor(new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), true));

        signLocations.put(player.getName(), loc.toVector());
        listeners.put(player.getName(), response);
    }

    public interface SignGUIListener {

        void onSignDone(Player player, String[] lines);
    }
}
