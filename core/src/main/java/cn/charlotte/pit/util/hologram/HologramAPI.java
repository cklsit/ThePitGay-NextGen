package cn.charlotte.pit.util.hologram;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.util.hologram.packet.PacketHologram;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class HologramAPI {

    protected static final List<Hologram> holograms = ObjectLists.synchronize(new ObjectArrayList<>());
    /**
     * 服务端版本判断。
     *
     * <p>原来用 1.8 时代的反射工具（{@code Minecraft.VERSION.newerThan(...)}）在静态初始化里探测版本，
     * 该工具在 Paper 1.21.8 上解析不到 {@code net.minecraft.server.UNKNOWN.Entity}，
     * 直接让本类初始化失败（NoClassDefFoundError），所有全息都无法工作。
     * 现在只支持 1.21 线，恒为 true。</p>
     */
    protected static boolean is1_8/*Or 1.9*/ = true;
    protected static boolean packetsEnabled = false;
    public static boolean isMAGA = true;

    private static boolean warnedMissingHook;

    /**
     * Creates a new {@link Hologram}
     *
     * @param loc  {@link Location} to spawn the hologram at
     * @param text Initial text content of the hologram
     * @return a new {@link Hologram}
     */
    public static Hologram createHologram(Location loc, String text) {
        Hologram hologram;
        if (ThePit.api != null) {
            hologram = ThePit.api.createHologram(loc, text);
        } else {
            // 外部 hook（ThePitSupport 一类附加插件）未安装时，退回本插件自带实现，避免全息整体不可用。
            if (!warnedMissingHook) {
                warnedMissingHook = true;
                ThePit.getInstance().getLogger().warning("PitInternalHook 未注册，全息改用内置 PacketHologram 实现");
            }
            hologram = new PacketHologram(text, loc);
            hologram.spawn();
        }
        holograms.add(hologram);
        return hologram;
    }

    /**
     * Removes a {@link Hologram}
     *
     * @param loc  {@link Location} of the hologram
     * @param text content of the hologram
     * @return <code>true</code> if a hologram was found and has been removed, <code>false</code> otherwise
     */
    public static boolean removeHologram(Location loc, String text) {
        Hologram toRemove = null;
        for (Hologram h : holograms) {
            if (h.getLocation().equals(loc) && h.getText().equals(text)) {
                toRemove = h;
                break;
            }
        }
        if (toRemove != null) {
            return removeHologram(toRemove);
        }
        return false;
    }

    /**
     * Removes a {@link Hologram}
     *
     * @param hologram {@link Hologram} to remove
     * @return <code>true</code> if the hologram has been removed
     */
    public static boolean removeHologram(@Nonnull Hologram hologram) {
        if (hologram.isSpawned()) {
            hologram.deSpawn();
        }
        if (hologram instanceof PacketHologram && isMAGA) {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    ((PacketHologram) hologram).getHologram().recycleEntity();
                });
            } else {
                ((PacketHologram) hologram).getHologram().recycleEntity();
            }
        }
        return holograms.remove(hologram);
    }

    /**
     * @return {@link Collection} of all registered {@link Hologram}s
     */
    public static Collection<Hologram> getHolograms() {
        return new ArrayList<>(holograms);
    }

    protected static boolean spawn(@Nonnull final Hologram hologram, final Collection<? extends Player> receivers) throws Exception {
        checkReceiverWorld(hologram, receivers);
        if (!receivers.isEmpty()) {
            if (hologram instanceof PacketHologram packetHologram) {
                receivers.forEach(i -> {
                    packetHologram.getHologram().show(i, false);
                });
            }
        }
        return true;
    }

    protected static boolean despawn(@Nonnull Hologram hologram, Collection<? extends Player> receivers) throws Exception {
        if (receivers.isEmpty()) {
            return false;
        }
        if (hologram instanceof PacketHologram packetHologram) {
            receivers.forEach(i -> {
                packetHologram.getHologram().show(i, false);
            });
        }
        return true;
    }

    protected static void sendPacket(Player p, Object packet) {
        // 1.8 的 playerConnection/sendPacket 路径在 1.21 已不存在；
        // 全息现在走 PacketArmorStand + hideEntity/showEntity，不再需要手工发包。
    }

    protected static Collection<? extends Player> checkReceiverWorld(final Hologram hologram, final Collection<? extends Player> receivers) {
        receivers.removeIf(next -> !next.getWorld().equals(hologram.getLocation().getWorld()));
        return receivers;
    }

    /** 1.21 下所有客户端都是现代版本，直接返回当前协议号占位。 */
    protected static int getVersion(Player p) {
        return 771;
    }

    public static boolean is1_8() {
        return is1_8;
    }

    public static boolean packetsEnabled() {
        return packetsEnabled;
    }

}
