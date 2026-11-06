package cn.charlotte.pit.bot;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 玩家模型的 Bot 渲染器（发包实现）。
 * <p>
 * Bukkit/Paper 没有提供生成玩家实体的 API，客户端只认玩家信息包 + 实体生成包，
 * 因此这里用 PacketEvents（随插件 shade 内置，无需服务端额外安装插件）完成：
 * <ol>
 *   <li>{@code PLAYER_INFO_UPDATE(ADD_PLAYER)} —— 写入名字与皮肤纹理，同时进入 Tab 列表</li>
 *   <li>{@code SPAWN_ENTITY(type=PLAYER)} —— 生成实体，客户端按玩家模型渲染</li>
 *   <li>{@code ENTITY_HEAD_ROTATION} —— 头部朝向</li>
 * </ol>
 * 移除时按相反顺序发送 {@code PLAYER_INFO_REMOVE} 与 {@code DESTROY_ENTITIES}。
 */
public final class PlayerBotRenderer {

    private final int entityId;
    private final UUID uuid;
    private final String name;
    private volatile BotSkin skin;
    private volatile Location location;

    public PlayerBotRenderer(String name, BotSkin skin, Location location) {
        this.name = name;
        this.skin = skin == null ? BotSkin.DEFAULT : skin;
        this.location = location.clone();
        this.uuid = BotSkin.offlineUuid(name);
        // 实体 id 取 UUID 混淆后的正数，避免与真实实体冲突的概率
        this.entityId = Math.abs(uuid.hashCode()) % 1_000_000 + 500_000;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location.clone();
    }

    public BotSkin getSkin() {
        return skin;
    }

    public void setSkin(BotSkin skin) {
        this.skin = skin == null ? BotSkin.DEFAULT : skin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRemove(player);
            sendSpawn(player);
        }
    }

    public void setLocation(Location location) {
        this.location = location.clone();
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRemove(player);
            sendSpawn(player);
        }
    }

    public void spawnFor(Player player) {
        sendSpawn(player);
    }

    public void despawnFor(Player player) {
        sendRemove(player);
    }

    public void spawnAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendSpawn(player);
        }
    }

    public void despawnAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRemove(player);
        }
    }

    private UserProfile profile() {
        UserProfile profile = new UserProfile(uuid, name);
        if (!skin.isEmpty()) {
            profile.setTextureProperties(List.of(
                    new TextureProperty("textures", skin.getValue(), skin.getSignature())));
        }
        return profile;
    }

    private void sendSpawn(Player player) {
        UserProfile profile = profile();
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo data =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile,
                        true,
                        0,
                        GameMode.SURVIVAL,
                        Component.text(name),
                        null);

        WrapperPlayServerPlayerInfoUpdate info = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME),
                List.of(data));

        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.PLAYER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.empty());

        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(
                entityId, location.getYaw());

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, info);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, head);
    }

    private void sendRemove(Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerPlayerInfoRemove(uuid));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerDestroyEntities(entityId));
    }
}
