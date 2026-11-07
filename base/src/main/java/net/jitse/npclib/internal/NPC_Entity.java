package net.jitse.npclib.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import net.jitse.npclib.hologram.Hologram;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * 1.21 的 NPC 实现：以「假玩家」呈现。
 * <p>
 * 客户端只认玩家信息包 + 玩家实体生成包，因此这里用 PacketEvents（随插件 shade 内置）发包：
 * <ol>
 *   <li>{@code PLAYER_INFO_UPDATE(ADD_PLAYER)} —— 写入名字与皮肤纹理</li>
 *   <li>{@code SPAWN_ENTITY(type=PLAYER)} —— 客户端按玩家模型渲染（不再是僵尸）</li>
 *   <li>{@code ENTITY_HEAD_ROTATION} —— 头部朝向</li>
 * </ol>
 * 点击交互由 {@link net.jitse.npclib.listeners.PacketListener}（ProtocolLib）拦截 {@code USE_ENTITY} 处理。
 */
public class NPC_Entity extends NPCBase {

    public NPC_Entity(NPCLib instance, List<String> lines) {
        super(instance, lines);
    }

    @Override
    public void createPackets() {
        // 无真实实体：假玩家完全由发包呈现，无需在服务端生成实体
    }

    @Override
    public void createPackets(Player player) {
        // 无真实实体
    }

    @Override
    public void sendShowPackets(Player player) {
        sendSpawn(player);
        Hologram hologram = getHologram(player);
        if (hologram != null) {
            hologram.show(player);
        }
    }

    @Override
    public void sendHidePackets(Player player) {
        sendRemove(player);
        Hologram hologram = getHologram(player);
        if (hologram != null) {
            hologram.hide(player);
        }
    }

    @Override
    public void sendMetadataPacket(Player player) {
        // 玩家模型无需额外元数据
    }

    @Override
    public void sendEquipmentPacket(Player player, NPCSlot slot, boolean auto) {
        // 手持物/装备暂不通过实体发包
    }

    @Override
    public void sendAnimationPacket(Player player, NPCAnimation animation) {
        // 动画暂不处理
    }

    @Override
    public void updateSkin(Skin skin) {
        this.skin = skin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isShown(player)) {
                sendRemove(player);
                sendSpawn(player);
            }
        }
    }

    @Override
    public void sendHeadRotationPackets(Location location) {
        if (location == null) {
            return;
        }
        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(entityId, location.getYaw());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isShown(player)) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, head);
            }
        }
    }

    @Override
    public net.jitse.npclib.api.NPC setLocation(Location location) {
        super.setLocation(location);
        return this;
    }

    @Override
    public void destroy() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isShown(player)) {
                sendRemove(player);
            }
        }
        super.destroy();
    }

    private UserProfile profile() {
        UserProfile profile = new UserProfile(uuid, name);
        if (skin != null && skin.getValue() != null && !skin.getValue().isEmpty()) {
            profile.setTextureProperties(List.of(
                    new TextureProperty("textures", skin.getValue(), skin.getSignature())));
        }
        return profile;
    }

    private void sendSpawn(Player player) {
        Location location = getLocation();
        if (location == null) {
            return;
        }
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

        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(entityId, location.getYaw());

        // 皮肤各层（头发/外套/袖/裤）需通过元数据显式开启，否则客户端只渲染基础层导致"没头发"
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(
                entityId,
                List.of(new EntityData(17, EntityDataTypes.BYTE, (byte) 0x7F)));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, info);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, head);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    private void sendRemove(Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerPlayerInfoRemove(uuid));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerDestroyEntities(entityId));
    }
}
