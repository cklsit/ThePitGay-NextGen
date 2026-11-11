/*
 * Copyright (c) 2018 Jitse Boonstra
 */

package cn.mythicland.npclib.nms.packetevents;

import cn.mythicland.npclib.NPCLib;
import cn.mythicland.npclib.api.skin.Skin;
import cn.mythicland.npclib.api.state.NPCAnimation;
import cn.mythicland.npclib.api.state.NPCSlot;
import cn.mythicland.npclib.api.state.NPCState;
import cn.mythicland.npclib.hologram.Hologram;
import cn.mythicland.npclib.internal.MinecraftVersion;
import cn.mythicland.npclib.internal.NPCBase;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.entity.villager.VillagerData;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfession;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfessions;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.mojang.authlib.properties.Property;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Packet-based NPC implementation on top of PacketEvents.
 * <p>
 * 1.21 迁移：原先通过 1.8 的 {@code EntityPlayer} / {@code EntityVillager} 等 NMS 实体拼装
 * DataWatcher 与各类 {@code PacketPlayOut*}，再由 {@code PlayerConnection} 下发；
 * 现改用 PacketEvents 的 wrapper，并通过 {@code PlayerManager#sendPacket} 下发，
 * 不再依赖任何 NMS 类。
 */
public class NPC_PacketEvents extends NPCBase {

    private static final long PROFILE_REMOVE_DELAY_TICKS = 20L;

    /*
     * Paper 1.21.8 的实体元数据索引（按 SynchedEntityData 的注册顺序推导）：
     * Entity 0-7（0 为共享实体标志），LivingEntity 8-14，Player 15-20（17 为皮肤层），
     * AbstractVillager 等继承链之后的 Villager 为 18。
     */
    private static final int ENTITY_FLAGS_INDEX = 0;
    private static final int PLAYER_SKIN_PARTS_INDEX = 17;
    private static final int VILLAGER_DATA_INDEX = 18;
    private static final byte ALL_SKIN_PARTS = 0x7F;
    private static final int VILLAGER_LEVEL = 1;

    private final Set<UUID> profileViewers = new HashSet<>();
    private final Map<UUID, Long> profileGenerations = new HashMap<>();
    private final Map<UUID, BukkitTask> profileTasks = new HashMap<>();
    private long nextProfileGeneration;
    private final Villager.Profession villagerProfession;

    private WrapperPlayServerSpawnEntity spawnPacket;
    private WrapperPlayServerEntityHeadLook headLookPacket;
    private WrapperPlayServerDestroyEntities destroyPacket;
    private WrapperPlayServerTeams teamRegisterPacket;
    private WrapperPlayServerPlayerInfoUpdate playerInfoAddPacket;
    private WrapperPlayServerPlayerInfoRemove playerInfoRemovePacket;

    public NPC_PacketEvents(NPCLib instance, List<String> lines) {
        this(instance, lines, null);
    }

    private NPC_PacketEvents(NPCLib instance, List<String> lines, Villager.Profession villagerProfession) {
        super(instance, lines);
        this.villagerProfession = villagerProfession;
    }

    public static NPC_PacketEvents villager(NPCLib instance, List<String> lines, Villager.Profession profession) {
        return new NPC_PacketEvents(instance, lines, Objects.requireNonNull(profession));
    }

    @Override
    public Hologram getHologram(Player player) {
        Hologram holo = super.getHologram(player);
        if (holo == null) {
            holo = new Hologram(
                    MinecraftVersion.V1_8_R3,
                    location.clone().add(0, 0.5, 0),
                    getText(player)
            );
        }
        super.playerHologram.put(player.getUniqueId(), holo);
        return holo;
    }

    @Override
    public void createPackets() {
        createPackets(null);
    }

    @Override
    public void createPackets(Player player) {
        this.destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
        this.headLookPacket = new WrapperPlayServerEntityHeadLook(entityId, location.getYaw());

        if (this.villagerProfession != null) {
            this.spawnPacket = createVillagerSpawnPacket();
            this.teamRegisterPacket = null;
            this.playerInfoAddPacket = null;
            this.playerInfoRemovePacket = null;
            return;
        }

        this.teamRegisterPacket = new WrapperPlayServerTeams(
                getTeamName(),
                WrapperPlayServerTeams.TeamMode.CREATE,
                Optional.of(new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.empty(),
                        Component.empty(),
                        Component.empty(),
                        WrapperPlayServerTeams.NameTagVisibility.NEVER,
                        WrapperPlayServerTeams.CollisionRule.ALWAYS,
                        NamedTextColor.WHITE,
                        WrapperPlayServerTeams.OptionData.NONE
                )),
                List.of(name)
        );
        this.playerInfoAddPacket = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE
                ),
                List.of(new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        buildUserProfile(), false, 0, GameMode.SURVIVAL, null, null
                ))
        );
        this.playerInfoRemovePacket = new WrapperPlayServerPlayerInfoRemove(uuid);
        this.spawnPacket = createPlayerSpawnPacket();
    }

    @Override
    public void sendShowPackets(Player player) {
        if (this.villagerProfession != null) {
            send(player, spawnPacket);
            send(player, headLookPacket);
            getHologram(player).show(player);
            return;
        }

        UUID viewerUniqueId = player.getUniqueId();
        BukkitTask previousTask = profileTasks.remove(viewerUniqueId);
        if (previousTask != null) previousTask.cancel();
        long profileGeneration = ++this.nextProfileGeneration;
        this.profileGenerations.put(viewerUniqueId, profileGeneration);
        this.profileViewers.add(viewerUniqueId);

        if (hasTeamRegistered.add(player.getUniqueId()))
            send(player, teamRegisterPacket);
        send(player, playerInfoAddPacket);
        send(player, spawnPacket);
        send(player, headLookPacket);

        getHologram(player).show(player);

        profileTasks.put(viewerUniqueId, Bukkit.getScheduler().runTaskLater(
                instance.getPlugin(),
                () -> {
                    if (!this.isCurrentProfile(viewerUniqueId, profileGeneration)) return;
                    profileTasks.remove(viewerUniqueId);
                    if (!player.isOnline()) {
                        this.removeProfileState(viewerUniqueId, profileGeneration);
                        return;
                    }
                    if (!this.isCurrentProfile(viewerUniqueId, profileGeneration)) return;
                    this.removeProfile(player);
                },
                PROFILE_REMOVE_DELAY_TICKS
        ));
    }

    @Override
    public void teleport(Location destination) {
        if (!isCreated()) throw new IllegalStateException("NPC must be created before teleporting: " + getId());
        if (!getWorld().equals(destination.getWorld())) {
            throw new IllegalArgumentException("NPC teleport must remain in the same world: " + getId());
        }
        this.location = destination.clone();
        if (this.villagerProfession != null) {
            this.spawnPacket = this.createVillagerSpawnPacket();
        } else {
            this.spawnPacket = this.createPlayerSpawnPacket();
        }
        this.headLookPacket = new WrapperPlayServerEntityHeadLook(entityId, this.location.getYaw());

        for (Map.Entry<UUID, Hologram> entry : playerHologram.entrySet()) {
            Hologram hologram = entry.getValue();
            if (hologram == null) continue;
            List<Object> packets = hologram.move(this.location.clone().add(0, 0.5, 0));
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && isShown(viewer)) hologram.update(viewer, packets);
        }

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(this.location.getX(), this.location.getY(), this.location.getZ()),
                this.location.getYaw(),
                this.location.getPitch(),
                false
        );
        for (UUID viewerId : getShown()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !isShown(viewer)) continue;
            send(viewer, teleportPacket);
            send(viewer, headLookPacket);
        }
    }

    @Override
    public void sendHidePackets(Player player, boolean auto) {
        send(player, destroyPacket);

        Hologram hologram = playerHologram.get(player.getUniqueId());
        if (hologram != null) hologram.hide(player);

        this.removeProfile(player);
    }

    @Override
    public void sendMetadataPacket(Player player) {
        List<EntityData<?>> metadata = new ArrayList<>(2);
        metadata.add(new EntityData<>(ENTITY_FLAGS_INDEX, EntityDataTypes.BYTE, NPCState.getMasked(activeStates)));
        if (this.villagerProfession != null) {
            metadata.add(new EntityData<>(VILLAGER_DATA_INDEX, EntityDataTypes.VILLAGER_DATA,
                    new VillagerData(VillagerTypes.PLAINS, mapProfession(this.villagerProfession), VILLAGER_LEVEL)));
        } else {
            metadata.add(new EntityData<>(PLAYER_SKIN_PARTS_INDEX, EntityDataTypes.BYTE, ALL_SKIN_PARTS));
        }
        send(player, new WrapperPlayServerEntityMetadata(entityId, metadata));
    }

    @Override
    public void sendEquipmentPacket(Player player, NPCSlot slot, boolean auto) {
        ItemStack item = getItem(slot);
        List<Equipment> equipment = List.of(new Equipment(
                toEquipmentSlot(slot),
                SpigotConversionUtil.fromBukkitItemStack(item == null ? new ItemStack(Material.AIR) : item)
        ));
        send(player, new WrapperPlayServerEntityEquipment(entityId, equipment));
    }

    @Override
    public void sendAnimationPacket(Player player, NPCAnimation animation) {
        send(player, new WrapperPlayServerEntityAnimation(entityId, toAnimationType(animation)));
    }

    @Override
    public void playHurtFeedback() {
        super.playHurtFeedback();

        Location npcLocation = getLocation();
        if (npcLocation == null || npcLocation.getWorld() == null) return;
        WrapperPlayServerSoundEffect packet = new WrapperPlayServerSoundEffect(
                Sounds.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYER,
                new Vector3d(npcLocation.getX(), npcLocation.getY(), npcLocation.getZ()),
                1.0F,
                1.0F
        );
        for (UUID shownUuid : getShown()) {
            Player player = Bukkit.getPlayer(shownUuid);
            if (player == null || !isShown(player)) continue;
            send(player, packet);
        }
    }

    @Override
    public void updateSkin(Skin skin) {
        this.setSkin(skin);
        this.createPackets();
        for (UUID shownUuid : getShown()) {
            Player player = Bukkit.getPlayer(shownUuid);
            if (player == null || !isShown(player)) continue;
            sendHidePackets(player, false);
            sendShowPackets(player);
            sendMetadataPacket(player);
            sendEquipmentPackets(player);
        }
    }

    @Override
    public void onLogout(Player player) {
        super.onLogout(player);
        this.removeProfileState(player.getUniqueId());
    }

    @Override
    public void destroy() {
        Set<UUID> viewers = new HashSet<>(this.profileViewers);
        Set<UUID> teams = new HashSet<>(hasTeamRegistered);
        super.destroy();
        for (UUID viewerUniqueId : viewers) {
            Player player = Bukkit.getPlayer(viewerUniqueId);
            if (player == null || !player.isOnline()) continue;
            this.removeProfile(player);
        }
        this.profileViewers.clear();
        this.profileGenerations.clear();
        for (BukkitTask task : profileTasks.values()) task.cancel();
        profileTasks.clear();
        if (villagerProfession == null && !teams.isEmpty()) {
            WrapperPlayServerTeams remove = new WrapperPlayServerTeams(
                    getTeamName(),
                    WrapperPlayServerTeams.TeamMode.REMOVE,
                    Optional.empty(),
                    List.of()
            );
            for (UUID id : teams) {
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline()) send(player, remove);
            }
        }
    }

    @Override
    public void sendHeadRotationPackets(Location location) {
        for (UUID id : getShown()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !isShown(player)) continue;

            Location npcLocation = getLocation();
            Vector dirBetweenLocations = location.toVector().subtract(npcLocation.toVector());

            npcLocation.setDirection(dirBetweenLocations);

            float yaw = npcLocation.getYaw();
            float pitch = npcLocation.getPitch();

            send(player, new WrapperPlayServerEntityRotation(entityId, yaw, pitch, false));
            send(player, new WrapperPlayServerEntityHeadLook(entityId, yaw));
        }
    }

    private WrapperPlayServerSpawnEntity createPlayerSpawnPacket() {
        return new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.PLAYER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.of(Vector3d.zero())
        );
    }

    private WrapperPlayServerSpawnEntity createVillagerSpawnPacket() {
        return new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.VILLAGER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.of(Vector3d.zero())
        );
    }

    private UserProfile buildUserProfile() {
        List<TextureProperty> textures = new ArrayList<>();
        for (Property property : gameProfile.getProperties().get("textures")) {
            textures.add(new TextureProperty(property.name(), property.value(), property.signature()));
        }
        return new UserProfile(uuid, name, textures);
    }

    private String getTeamName() {
        return "npc_" + name;
    }

    private void removeProfile(Player player) {
        BukkitTask task = profileTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (!this.profileViewers.remove(player.getUniqueId())) return;
        this.profileGenerations.remove(player.getUniqueId());
        send(player, playerInfoRemovePacket);
    }

    private boolean isCurrentProfile(UUID viewerUniqueId, long profileGeneration) {
        return Long.valueOf(profileGeneration).equals(this.profileGenerations.get(viewerUniqueId));
    }

    private void removeProfileState(UUID viewerUniqueId, long profileGeneration) {
        if (!this.isCurrentProfile(viewerUniqueId, profileGeneration)) return;
        this.removeProfileState(viewerUniqueId);
    }

    private void removeProfileState(UUID viewerUniqueId) {
        BukkitTask task = profileTasks.remove(viewerUniqueId);
        if (task != null) task.cancel();
        this.profileGenerations.remove(viewerUniqueId);
        this.profileViewers.remove(viewerUniqueId);
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        if (player == null || packet == null) return;
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private static EquipmentSlot toEquipmentSlot(NPCSlot slot) {
        return switch (slot) {
            case HELMET -> EquipmentSlot.HELMET;
            case CHESTPLATE -> EquipmentSlot.CHEST_PLATE;
            case LEGGINGS -> EquipmentSlot.LEGGINGS;
            case BOOTS -> EquipmentSlot.BOOTS;
            case MAINHAND -> EquipmentSlot.MAIN_HAND;
            case OFFHAND -> EquipmentSlot.OFF_HAND;
        };
    }

    private static WrapperPlayServerEntityAnimation.EntityAnimationType toAnimationType(NPCAnimation animation) {
        return switch (animation) {
            case SWING_MAINHAND -> WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
            case TAKE_DAMAGE -> WrapperPlayServerEntityAnimation.EntityAnimationType.HURT;
            case SWING_OFFHAND -> WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND;
            case CRITICAL_DAMAGE -> WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT;
            case MAGICAL_DAMAGE -> WrapperPlayServerEntityAnimation.EntityAnimationType.MAGIC_CRITICAL_HIT;
        };
    }

    private static VillagerProfession mapProfession(Villager.Profession profession) {
        return switch (profession.getKey().getKey()) {
            case "armorer" -> VillagerProfessions.ARMORER;
            case "butcher" -> VillagerProfessions.BUTCHER;
            case "cartographer" -> VillagerProfessions.CARTOGRAPHER;
            case "cleric" -> VillagerProfessions.CLERIC;
            case "farmer" -> VillagerProfessions.FARMER;
            case "fisherman" -> VillagerProfessions.FISHERMAN;
            case "fletcher" -> VillagerProfessions.FLETCHER;
            case "leatherworker" -> VillagerProfessions.LEATHERWORKER;
            case "librarian" -> VillagerProfessions.LIBRARIAN;
            case "mason" -> VillagerProfessions.MASON;
            case "nitwit" -> VillagerProfessions.NITWIT;
            case "shepherd" -> VillagerProfessions.SHEPHERD;
            case "toolsmith" -> VillagerProfessions.TOOLSMITH;
            case "weaponsmith" -> VillagerProfessions.WEAPONSMITH;
            default -> VillagerProfessions.NONE;
        };
    }
}
