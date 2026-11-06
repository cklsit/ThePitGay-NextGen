package net.jitse.npclib.internal;

import net.jitse.npclib.NPCLib;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import net.jitse.npclib.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * 1.21 的 NPC 实现。
 * <p>
 * 原 {@code nms/v1_8_R3} 版本依赖 1.8 的幽灵实体与手写生成/元数据包；这里改为真实实体：
 * 僵尸作为人形主体（无 AI、无敌、静音、不持久化），玩家头颅承载皮肤，头顶文本用 {@link Hologram}（TextDisplay）。
 * 可见性通过 {@link Player#showEntity}/{@link Player#hideEntity} 按玩家控制。
 */
public class NPC_Entity extends NPCBase {

    private static final String NPC_TAG = "pit_npc";

    private Zombie body;

    public NPC_Entity(NPCLib instance, List<String> lines) {
        super(instance, lines);
    }

    @Override
    public void createPackets() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(getPlugin(), (Runnable) this::createPackets);
            return;
        }
        Location spawn = getLocation();
        if (spawn == null || spawn.getWorld() == null) {
            return;
        }
        if (body == null || !body.isValid()) {
            body = spawn.getWorld().spawn(spawn, Zombie.class, entity -> {
                entity.setPersistent(false);
                entity.setSilent(true);
                entity.setInvulnerable(true);
                entity.setAI(false);
                entity.setAdult();
                entity.setCollidable(false);
                entity.setRemoveWhenFarAway(false);
                entity.setShouldBurnInDay(false);
                entity.setCustomNameVisible(false);
                entity.addScoreboardTag(NPC_TAG);
            });
            applyEquipment();
        }
    }

    @Override
    public void createPackets(Player player) {
        createPackets();
    }

    @Override
    public void sendShowPackets(Player player) {
        if (body != null && body.isValid()) {
            player.showEntity(getPlugin(), body);
        }
        Hologram hologram = getHologram(player);
        if (hologram != null) {
            hologram.show(player);
        }
    }

    @Override
    public void sendHidePackets(Player player) {
        if (body != null && body.isValid()) {
            player.hideEntity(getPlugin(), body);
        }
        Hologram hologram = getHologram(player);
        if (hologram != null) {
            hologram.hide(player);
        }
    }

    @Override
    public void sendMetadataPacket(Player player) {
        if (body != null) {
            body.setCustomNameVisible(true);
        }
    }

    @Override
    public void sendEquipmentPacket(Player player, NPCSlot slot, boolean auto) {
        applyEquipment();
    }

    @Override
    public void sendAnimationPacket(Player player, NPCAnimation animation) {
        if (body == null) {
            return;
        }
        switch (animation) {
            case SWING_MAINHAND -> body.swingMainHand();
            case SWING_OFFHAND -> body.swingOffHand();
            case TAKE_DAMAGE -> body.playHurtAnimation(0);
            default -> {
            }
        }
    }

    @Override
    public void updateSkin(Skin skin) {
        this.skin = skin;
        applyEquipment();
    }

    @Override
    public void sendHeadRotationPackets(Location location) {
        if (body == null || location == null) {
            return;
        }
        Location current = body.getLocation();
        current.setDirection(location.toVector().subtract(current.toVector()));
        body.teleport(current);
    }

    @Override
    public net.jitse.npclib.api.NPC setLocation(Location location) {
        super.setLocation(location);
        if (body != null && body.isValid() && location != null) {
            body.teleport(location);
        }
        return this;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (body != null) {
            body.remove();
            body = null;
        }
    }

    private void applyEquipment() {
        if (body == null || !body.isValid()) {
            return;
        }
        EntityEquipment equipment = body.getEquipment();
        if (equipment == null) {
            return;
        }
        for (NPCSlot slot : NPCSlot.values()) {
            ItemStack item = getItem(slot);
            if (item == null) {
                continue;
            }
            switch (slot) {
                case HELMET -> equipment.setHelmet(withSkin(item));
                case CHESTPLATE -> equipment.setChestplate(item);
                case LEGGINGS -> equipment.setLeggings(item);
                case BOOTS -> equipment.setBoots(item);
                case MAINHAND -> equipment.setItemInMainHand(item);
                case OFFHAND -> equipment.setItemInOffHand(item);
                default -> {
                }
            }
        }
        if (getItem(NPCSlot.HELMET) == null) {
            equipment.setHelmet(withSkin(new ItemStack(Material.PLAYER_HEAD)));
        }
    }

    private ItemStack withSkin(ItemStack base) {
        Skin current = this.skin;
        if (current == null || current.getValue() == null || current.getValue().isEmpty()) {
            return base;
        }
        ItemStack stack = base.clone();
        if (!(stack.getItemMeta() instanceof SkullMeta meta)) {
            return base;
        }
        String url = cn.charlotte.pit.bot.BotSkin.extractUrl(current.getValue());
        if (url == null) {
            return base;
        }
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "PitNpc");
            profile.getTextures().setSkin(new URL(url));
            meta.setOwnerProfile(profile);
            stack.setItemMeta(meta);
        } catch (Exception ignored) {
        }
        return stack;
    }
}
