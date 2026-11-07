/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package net.jitse.npclib.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.events.NPCInteractEvent;
import net.jitse.npclib.internal.NPCBase;
import net.jitse.npclib.internal.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * NPC 交互监听。
 * <p>
 * NPC 是假玩家（客户端实体，无真实服务端实体），点击只能通过拦截
 * {@code USE_ENTITY} 包捕获。这里用 ProtocolLib 拦截并匹配假玩家实体 id。
 *
 * @author Jitse Boonstra
 */
public class PacketListener {

    // Prevent players from clicking at very high speeds.
    private final Set<UUID> delay = new HashSet<>();

    private Plugin plugin;

    public void start(NPCLib instance) {
        this.plugin = instance.getPlugin();
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(instance.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (event.getPacketType() != PacketType.Play.Client.USE_ENTITY) {
                            return;
                        }
                        int entityId = event.getPacket().getIntegers().read(0);
                        WrappedEnumEntityUseAction action = event.getPacket().getEnumEntityUseActions().read(0);
                        handleInteractPacket(event.getPlayer(), entityId, action);
                    }
                });
    }

    private void handleInteractPacket(Player player, int packetEntityId, WrappedEnumEntityUseAction action) {
        NPCBase npc = null;

        for (NPCBase testNPC : NPCManager.getAllNPCs()) {
            if (testNPC.isCreated() && testNPC.isShown(player) && testNPC.getEntityId() == packetEntityId) {
                npc = testNPC;
                break;
            }
        }

        if (npc == null) {
            return;
        }

        if (delay.contains(player.getUniqueId())) {
            return;
        }

        NPCInteractEvent.ClickType clickType = "ATTACK".equals(action.getAction().name())
                ? NPCInteractEvent.ClickType.LEFT_CLICK : NPCInteractEvent.ClickType.RIGHT_CLICK;

        delay.add(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, new TaskCallNpcInteractEvent(new NPCInteractEvent(player, clickType, npc), this));
    }

    private static final class TaskCallNpcInteractEvent implements Runnable {
        private final NPCInteractEvent eventToCall;
        private final PacketListener listener;

        TaskCallNpcInteractEvent(NPCInteractEvent eventToCall, PacketListener listener) {
            this.eventToCall = eventToCall;
            this.listener = listener;
        }

        @Override
        public void run() {
            Player player = eventToCall.getWhoClicked();
            this.listener.delay.remove(player.getUniqueId());
            if (player.getWorld() != eventToCall.getNPC().getWorld())
                return;

            double distance = player.getLocation().distanceSquared(eventToCall.getNPC().getLocation());
            if (distance <= 64)
                Bukkit.getPluginManager().callEvent(this.eventToCall);
        }
    }
}
