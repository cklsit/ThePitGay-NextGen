/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package net.jitse.npclib.listeners;

import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.events.NPCInteractEvent;
import net.jitse.npclib.internal.NPCBase;
import net.jitse.npclib.internal.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * NPC 交互监听。
 * <p>
 * 1.21 适配：NPC 已改为真实实体，这里用 Bukkit 的实体交互/攻击事件替代
 * ProtocolLib 的 {@code USE_ENTITY} 包拦截，彻底去掉对 ProtocolLib 的依赖。
 *
 * @author Jitse Boonstra
 */
public class PacketListener implements Listener {

    // Prevent players from clicking at very high speeds.
    private final Set<UUID> delay = new HashSet<>();

    private Plugin plugin;

    public void start(NPCLib instance) {
        this.plugin = instance.getPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked().getEntityId(), NPCInteractEvent.ClickType.RIGHT_CLICK);
    }

    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            handleInteract(player, event.getEntity().getEntityId(), NPCInteractEvent.ClickType.LEFT_CLICK);
        }
    }

    private void handleInteract(Player player, int packetEntityId, NPCInteractEvent.ClickType clickType) {
        NPCBase npc = null;

        // Not using streams here is an intentional choice.
        for (NPCBase testNPC : NPCManager.getAllNPCs()) {
            if (testNPC.isCreated() && testNPC.isShown(player) && testNPC.getEntityId() == packetEntityId) {
                npc = testNPC;
                break;
            }
        }

        if (npc == null) {
            // Default entity, not doing magic.
            return;
        }

        if (delay.contains(player.getUniqueId())) {
            return;
        }

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
