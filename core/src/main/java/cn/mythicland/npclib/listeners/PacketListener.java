/* Copyright (c) 2018 Jitse Boonstra. */
package cn.mythicland.npclib.listeners;

import cn.mythicland.npclib.NPCLib;
import cn.mythicland.npclib.api.events.NPCInteractEvent;
import cn.mythicland.npclib.internal.NPCBase;
import cn.mythicland.npclib.internal.NPCManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * NPC 点击检测。
 *
 * <p>原生实现只走 ProtocolLib 的 {@code USE_ENTITY} 包；1.21.8 上 ProtocolLib 5.4.0
 * 在 Paper 上不可用（异步监听直接抛错），会导致所有数据包 NPC 点不动。
 * 现在优先用插件内置的 PacketEvents 监听同一个客户端包，ProtocolLib 仅作兜底。</p>
 */
public class PacketListener {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private NPCLib instance;
    private ProtocolManager protocol;
    private PacketAdapter adapter;
    private PacketListenerAbstract packetEventsListener;
    private volatile boolean active;

    public void start(NPCLib instance) {
        this.instance = instance;
        this.active = true;
        if (startPacketEvents()) {
            return;
        }
        startProtocolLib();
    }

    /**
     * @return true 表示 PacketEvents 已接管点击检测
     */
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

    private void startProtocolLib() {
        try {
            this.protocol = ProtocolLibrary.getProtocolManager();
            adapter = new PacketAdapter(instance.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
                @Override public void onPacketReceiving(PacketEvent event) {
                    if (event.isCancelled() || !active) return;
                    Integer id = event.getPacket().getIntegers().readSafely(0);
                    EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().readSafely(0);
                    if (id == null || action == null) return;
                    handleInteract(event.getPlayer(), id, action == EnumWrappers.EntityUseAction.ATTACK);
                }
            };
            protocol.addPacketListener(adapter);
        } catch (Throwable throwable) {
            // ProtocolLib 不存在或版本不兼容：NPC 点击已由 PacketEvents 处理
            adapter = null;
            protocol = null;
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
        if (adapter != null && protocol != null) {
            try { protocol.removePacketListener(adapter); } catch (Throwable ignored) { }
        }
        adapter = null;
        protocol = null;
        pending.clear();
        lastClick.clear();
    }
}
