/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package cn.mythicland.npclib.hologram;

import cn.mythicland.npclib.internal.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 1.21 迁移：原先通过 1.8 的 EntityArmorStand + PacketPlayOut* 发包实现，
 * 现改为真实盔甲架实体（marker + 不可见），并用 Bukkit 的
 * {@link Player#showEntity} / {@link Player#hideEntity} 实现按玩家显隐，
 * 保持原有的 show / hide / move 语义。
 */
public class Hologram {

    private static final double DELTA = 0.3;

    private final List<ArmorStand> stands = new ArrayList<>();
    private final Set<UUID> shown = new HashSet<>();
    private Location location;
    private List<String> text;

    public Hologram(MinecraftVersion version, Location location, List<String> text) {
        this.location = location.clone();
        this.text = text == null ? new ArrayList<>() : new ArrayList<>(text);
        spawnStands();
    }

    private void spawnStands() {
        removeStands();
        if (location.getWorld() == null) return;
        for (int i = 0; i < text.size(); i++) {
            ArmorStand stand = location.getWorld().spawn(location.clone().subtract(0, DELTA * i, 0), ArmorStand.class);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setCollidable(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(colorize(text.get(i)));
            stands.add(stand);
            // 默认对所有在线玩家隐藏，等 show(player) 再单独放出
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.hideEntity(thePit(), stand);
            }
        }
    }

    private void removeStands() {
        for (ArmorStand stand : stands) {
            if (!stand.isDead()) stand.remove();
        }
        stands.clear();
        shown.clear();
    }

    private static org.bukkit.plugin.Plugin thePit() {
        return Bukkit.getPluginManager().getPlugin("ThePitUltimate");
    }

    private static String colorize(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    /** 文本更新：直接改实体名称，返回占位列表以兼容原有调用约定。 */
    public List<Object> getUpdatePackets(List<String> text) {
        this.text = text == null ? new ArrayList<>() : new ArrayList<>(text);
        if (stands.size() != this.text.size()) {
            spawnStands();
        } else {
            for (int i = 0; i < stands.size(); i++) {
                stands.get(i).setCustomName(colorize(this.text.get(i)));
            }
        }
        List<Object> marker = new ArrayList<>();
        marker.add(Boolean.TRUE);
        return marker;
    }

    public List<Object> move(Location destination) {
        this.location = destination.clone();
        for (int i = 0; i < stands.size(); i++) {
            stands.get(i).teleport(location.clone().subtract(0, DELTA * i, 0));
        }
        List<Object> marker = new ArrayList<>();
        marker.add(Boolean.TRUE);
        return marker;
    }

    public void update(Player player, List<Object> updatePackets) {
        if (player == null) return;
        for (ArmorStand stand : stands) {
            player.showEntity(thePit(), stand);
        }
    }

    public void show(Player player) {
        if (player == null) return;
        shown.add(player.getUniqueId());
        for (ArmorStand stand : stands) {
            player.showEntity(thePit(), stand);
        }
    }

    public void hide(Player player) {
        if (player == null) return;
        shown.remove(player.getUniqueId());
        for (ArmorStand stand : stands) {
            player.hideEntity(thePit(), stand);
        }
    }

    /** 供 NPC 删除时清理实体。 */
    public void destroy() {
        removeStands();
    }
}
