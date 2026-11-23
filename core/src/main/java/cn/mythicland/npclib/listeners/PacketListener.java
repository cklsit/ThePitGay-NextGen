/* Copyright (c) 2018 Jitse Boonstra. */
package cn.mythicland.npclib.listeners;

import cn.mythicland.npclib.NPCLib;
import cn.mythicland.npclib.api.events.NPCInteractEvent;
import cn.mythicland.npclib.internal.NPCBase;
import cn.mythicland.npclib.internal.NPCManager;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * NPC 点击检测。
 *
 * <p>只依赖插件内置（并已重定位）的 PacketEvents，监听客户端 {@code INTERACT_ENTITY} 包。
 * 早先实现引用了 ProtocolLib 类，未安装 ProtocolLib 时本类会 NoClassDefFoundError，
 * 连带 NPCLib 构造失败、整个插件被禁用，因此这里彻底移除 ProtocolLib 引用。</p>
 */
public class PacketListener {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private NPCLib instance;
    private PacketListenerAbstract packetEventsListener;
    private volatile boolean active;

    public void start(NPCLib instance) {
        this.instance = instance;
        this.active = true;
        if (startPacketEvents()) {
            return;
        }
        instance.getLogger().warning("PacketEvents 未就绪，NPC 点击检测不可用");
    }

    private boolean startPacketEvents() {
        try {
            com.github.retrooper.packetevents.PacketEventsAPI api =
                    com.github.retrooper.packetevents.PacketEvents.getAPI();
            if (api == null || !api.isInitialized()) {
                return false;
            }
            packetEventsListener = new PacketListenerAbstract() {
                @Override
                public void onPacketReceive(PacketReceiveEvent event) {
                    if (!active || event.isCancelled()) return;
                    if (event.getPacketType() != com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.INTERACT_ENTITY) {
                        return;
                    }
                    Player player = event.getPlayer();
                    if (player == null) return;
                    WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
                    handleInteract(player, wrapper.getEntityId(),
                            wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);
                }
            };
            api.getEventManager().registerListener(packetEventsListener);
            return true;
        } catch (Throwable throwable) {
            packetEventsListener = null;
            return false;
        }
    }

    void handleInteract(Player player, int entityId, boolean attack) {
        if (!active) return;
        // Only the concurrent entity-ID registry is consulted off the Bukkit thread.
        NPCBase npc = NPCManager.getByEntityId(entityId);
        if (npc == null || npc.getInstance() != instance) return;
        UUID id = player.getUniqueId();
        if (!pending.add(id)) return;
        Runnable dispatch = () -> {
            try {
                if (!active || instance.isShutdown() || !player.isOnline() || player.isDead()
                        || NPCManager.getByEntityId(entityId) != npc || !npc.isCreated() || !npc.isShown(player)
                        || !player.getWorld().equals(npc.getWorld())
                        || player.getLocation().distanceSquared(npc.getLocation()) > 64) return;
                long now = System.nanoTime();
                Long previous = lastClick.get(id);
                if (previous != null && now - previous < TimeUnit.MILLISECONDS.toNanos(100)) return;
                lastClick.put(id, now);
                Bukkit.getPluginManager().callEvent(new NPCInteractEvent(player, attack
                        ? NPCInteractEvent.ClickType.LEFT_CLICK : NPCInteractEvent.ClickType.RIGHT_CLICK, npc));
            } finally {
                pending.remove(id);
            }
        };
        if (Bukkit.isPrimaryThread()) dispatch.run();
        else {
            try { Bukkit.getScheduler().runTask(instance.getPlugin(), dispatch); }
            catch (org.bukkit.plugin.IllegalPluginAccessException disabled) { pending.remove(id); }
        }
    }

    public void forget(Player player) {
        pending.remove(player.getUniqueId());
        lastClick.remove(player.getUniqueId());
    }

    public void stop() {
        active = false;
        if (packetEventsListener != null) {
            try {
                com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                        .unregisterListener(packetEventsListener);
            } catch (Throwable ignored) {
            }
            packetEventsListener = null;
        }
        pending.clear();
        lastClick.clear();
    }
}
