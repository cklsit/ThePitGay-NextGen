package net.mizukilab.pit.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备伪装：让其他客户端把目标玩家的盔甲看成空，或回滚为真实装备。
 * <p>
 * 1.21 适配：原先手写 {@code PacketPlayOutEntityEquipment} 并强转 CraftPlayer，
 * 现改为 PacketEvents 的装备包（已内置），不再依赖 NMS。
 *
 * @author Araykal
 * @since 2025/1/17
 */
public class VisibleApi {

    private static void sendEquipment(Player target, Player viewer, EquipmentSlot slot, ItemStack item) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(
                target.getEntityId(),
                List.of(new Equipment(slot, SpigotConversionUtil.fromBukkitItemStack(
                        item == null ? new ItemStack(Material.AIR) : item))));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    public static void hideArmor(Player player) {
        hideNameTag(player);
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.BOOTS, EquipmentSlot.LEGGINGS,
                    EquipmentSlot.CHEST_PLATE, EquipmentSlot.HELMET}) {
                sendEquipment(player, target, slot, new ItemStack(Material.AIR));
            }
        }
    }

    public static void hidePlayerFromAll(Player target) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(target)) {
                player.hidePlayer(target);
            }
        }
    }

    public static void showPlayerToAll(Player target) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(target)) {
                player.showPlayer(target);
            }
        }
    }

    private static void hideNameTag(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("hiddenNameTags");

        if (team == null) {
            team = scoreboard.registerNewTeam("hiddenNameTags");
            team.setNameTagVisibility(NameTagVisibility.NEVER);
        }

        team.addEntry(player.getName());
    }

    private static void showNameTag(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("hiddenNameTags");

        if (team != null && team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
        }
    }

    public static void showArmor(Player player) {
        List<Equipment> equipment = new ArrayList<>(4);
        equipment.add(new Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getBoots())));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getLeggings())));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getChestplate())));
        equipment.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(player.getInventory().getHelmet())));

        showNameTag(player);
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(player.getEntityId(), equipment);
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player)) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(target, packet);
            }
        }
    }

    /**
     * 仅对其他人伪装目标玩家的头盔（本人视角不变）。
     */
    public static void setFakeHelmet(Player target, ItemStack helmet) {
        if (helmet == null) {
            return;
        }
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(
                target.getEntityId(),
                List.of(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(helmet))));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }
}
