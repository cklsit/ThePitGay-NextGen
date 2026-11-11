package cn.charlotte.pit.util.hologram.packet;

import cn.charlotte.pit.ThePit;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 2022/2/26<br>
 * LimeCode<br>
 * <p>
 * 1.21 适配：原先依赖 NMS 幽灵实体与手写生成包，现改为真实 {@link ArmorStand} 实体，
 * 可见性通过 {@link Player#hideEntity}/{@link Player#showEntity} 控制，语义与原实现一致。
 *
 * @author huanmeng_qwq
 */
public class PacketArmorStand {

    public static final double DISTANCE = 64;
    public static final String HOLOGRAM_TAG = "pit_hologram";

    protected String text;
    public Location location;
    protected volatile ArmorStand entity;
    protected final Set<Player> users;
    protected final Set<Player> viewing;
    protected final Set<Player> hiding;

    protected boolean marker = false;
    protected boolean basePlate = true;
    protected boolean small = false;
    protected boolean visible = true;
    protected boolean nameVisible = true;

    public PacketArmorStand(String text, Location location) {
        this.text = text;
        this.location = location;
        this.users = new ConcurrentHashSet<>();
        this.viewing = new ConcurrentHashSet<>();
        this.hiding = new ConcurrentHashSet<>();
        init();
        // 主线程下沿用「构造即持有实体」的旧语义（附着实体的调用方在 spawn 后立即读取实体 ID）；
        // 异步创建时交给 ensureEntity 延后到主线程执行。
        if (Bukkit.isPrimaryThread()) {
            ensureEntity();
        }
    }

    public void init() {
        this.nameVisible = !text.isEmpty();
    }

    public void invisible() {
        this.visible = false;
    }

    public void small() {
        this.small = true;
    }

    public void marker() {
        this.marker = true;
    }

    public void basePlate(boolean basePlate) {
        this.basePlate = basePlate;
    }

    /**
     * 惰性创建真实实体；异步线程下会调度到主线程并返回 null，调用方需容忍 null。
     */
    @Nullable
    protected ArmorStand ensureEntity() {
        ArmorStand current = this.entity;
        if (current != null && current.isValid() && !current.isDead()) {
            return current;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), this::ensureEntity);
            return null;
        }
        Location spawnLocation = this.location.clone();
        if (spawnLocation.getWorld() == null) {
            return null;
        }
        ArmorStand spawned = spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class, stand -> {
            stand.setPersistent(false);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.setMarker(this.marker);
            stand.setBasePlate(this.basePlate);
            stand.setSmall(this.small);
            stand.setVisible(this.visible);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(this.nameVisible && !this.text.isEmpty());
            if (!this.text.isEmpty()) {
                stand.setCustomName(this.text);
            }
            stand.addScoreboardTag(HOLOGRAM_TAG);
        });
        this.entity = spawned;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!users.contains(player)) {
                player.hideEntity(ThePit.getInstance(), spawned);
            }
        }
        return spawned;
    }

    public void redisplay(Player user, boolean force) {
        hide(user, force);
        show(user, force);
    }

    public void addUser(Player user) {
        if (users.add(user)) {
            show(user, false);
        }
    }

    public void removeUser(Player user) {
        if (users.remove(user)) {
            hide(user, true);
        }
        hiding.remove(user);
        viewing.remove(user);
    }

    public void recycleEntity() {
        ArmorStand current = this.entity;
        this.entity = null;
        if (current != null) {
            current.remove();
        }
    }

    public void removeAll() {
        ArrayList<Player> list = new ArrayList<>(users);
        for (Player user : list) {
            removeUser(user);
        }
        viewing.clear();
        hiding.clear();
        users.clear();
    }

    public void show(Player user, boolean force) {
        if (!user.getWorld().equals(location.getWorld())) {
            hiding.add(user);
            viewing.remove(user);
            return;
        }
        if (user.getLocation().distanceSquared(location) >= DISTANCE * DISTANCE) {
            hiding.add(user);
            viewing.remove(user);
            return;
        }
        if (viewing.add(user) || force) {
            ArmorStand current = ensureEntity();
            if (current != null) {
                user.showEntity(ThePit.getInstance(), current);
            }
            hiding.remove(user);
        }
    }

    /**
     * 1.21 下装备由实体自身同步，无需手工发包，保留方法仅为兼容旧调用。
     */
    public void sendEquipments(Player user) {
        ArmorStand current = ensureEntity();
        if (current != null) {
            user.showEntity(ThePit.getInstance(), current);
        }
    }

    private List<Pair<EquipmentSlot, ItemStack>> getItems(LivingEntity entity) {
        List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>(6);
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return list;
        }
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            switch (equipmentSlot) {
                case HAND:
                    list.add(new Pair<>(equipmentSlot, equipment.getItemInMainHand()));
                    break;
                case OFF_HAND:
                    list.add(new Pair<>(equipmentSlot, equipment.getItemInOffHand()));
                    break;
                case FEET:
                    list.add(new Pair<>(equipmentSlot, equipment.getBoots()));
                    break;
                case LEGS:
                    list.add(new Pair<>(equipmentSlot, equipment.getLeggings()));
                    break;
                case CHEST:
                    list.add(new Pair<>(equipmentSlot, equipment.getChestplate()));
                    break;
                case HEAD:
                    list.add(new Pair<>(equipmentSlot, equipment.getHelmet()));
                    break;
                default:
                    break;
            }
        }
        return list;
    }

    public void hide(Player user, boolean force) {
        if (!user.getWorld().equals(location.getWorld())) {
            hiding.add(user);
            viewing.remove(user);
            return;
        }
        if (!force && user.getLocation().distanceSquared(location) >= DISTANCE * DISTANCE) {
            hiding.add(user);
            viewing.remove(user);
            return;
        }
        if (hiding.add(user) || force) {
            ArmorStand current = this.entity;
            if (current != null) {
                user.hideEntity(ThePit.getInstance(), current);
            }
            viewing.remove(user);
        }
    }

    private void clean() {
        viewing.removeIf(e -> !e.isOnline());
        hiding.removeIf(e -> !e.isOnline());
        users.removeIf(e -> !e.isOnline());
    }

    public void update() {
        clean();
        ArmorStand current = ensureEntity();
        if (current == null) {
            return;
        }
        updateView();
        current.setCustomName(this.text);
        current.setCustomNameVisible(this.nameVisible && !this.text.isEmpty());
    }

    public void updateView() {
        ArrayList<Player> viewingList = new ArrayList<>(viewing);
        ArrayList<Player> hidingList = new ArrayList<>(hiding);
        for (Player user : viewingList) {
            if (!user.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (user.getLocation().distanceSquared(location) >= DISTANCE * DISTANCE) {
                hide(user, false);
            } else {
                show(user, false);
            }
        }
        for (Player user : hidingList) {
            if (!user.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (user.getLocation().distanceSquared(location) < DISTANCE * DISTANCE) {
                show(user, false);
            }
        }
    }

    public void location(@NotNull Location location) {
        ArmorStandHelper.applyLocation(location, this);
    }

    public String text() {
        return text;
    }

    public PacketArmorStand setText(String text) {
        return this.setText(text, true);
    }

    public PacketArmorStand setText(String text, boolean update) {
        if (this.text.equals(text)) {
            return this;
        }
        this.text = text;
        ArmorStand current = this.entity;
        if (current != null) {
            if (!text.isEmpty()) {
                current.setCustomName(text);
                current.setCustomNameVisible(this.nameVisible);
            } else {
                current.setCustomNameVisible(false);
            }
        }
        if (update) {
            update();
        }
        return this;
    }

    public PacketArmorStand setNameVisible(boolean visible) {
        this.nameVisible = visible;
        ArmorStand current = this.entity;
        if (current != null) {
            current.setCustomNameVisible(visible);
        }
        return this;
    }

    public Location location() {
        return location;
    }

    public Set<Player> users() {
        return users;
    }

    public ArmorStand entity() {
        return ensureEntity();
    }

    public Set<Player> viewing() {
        return viewing;
    }

    public void sendTo(Location to, boolean ground) {
        move(to, ground);
    }

    public void move(Location to, boolean ground) {
        Location target = to.clone();
        this.location = target.clone();
        ArmorStand current = this.entity;
        if (current == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> current.teleport(target));
            return;
        }
        current.teleport(target);
    }

    /**
     * 真实实体使用传送同步，旧的相对位移包逻辑不再需要。
     */
    public void sendTeleportPacket(Location location, boolean ground) {
        move(location, ground);
    }

    public static byte getFixRotation(final float yawpitch) {
        return (byte) (yawpitch * 256.0F / 360.0F);
    }

    public void senHeadYaw(float yaw) {
        // 真实实体朝向随传送同步
    }
}
