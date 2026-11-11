package cn.mythicland.npclib.bot.v1_8_R3;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 按玩家裁剪机器人可见性。
 *
 * <p>机器人是真实登录的 {@link ServerPlayer}，服务端自己处理实体追踪，因此这里只负责
 * “该不该让这个玩家看到”这一层判断，并落到 {@link Player#showPlayer}/{@link Player#hidePlayer}。
 * 视野、距离、俯仰角、视线和挂机判定沿用原实现，既避免远处玩家被机器人刷屏，
 * 也保证战斗中的玩家始终能看到对手。</p>
 */
public class BotVisibility {
    private static final int MAX_REVEALS_PER_TICK = 16;

    private final Plugin plugin;
    private final Map<UUID, BotVisibilitySettings> configurations = new HashMap<>();
    private final Map<UUID, Viewer> viewers = new LinkedHashMap<>();
    private final Map<UUID, Activity> activity = new HashMap<>();
    private final Set<UUID> online = new HashSet<>();
    private long currentTick;
    private long revealTick = Long.MIN_VALUE;
    private int reveals, revealCursor;

    public BotVisibility(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    private static double angle(double degrees) {
        return ((degrees + 180) % 360 + 360) % 360 - 180;
    }

    public void configure(UUID bot, BotVisibilitySettings config) {
        BotVisibilitySettings previous = configurations.put(bot, Objects.requireNonNull(config, "config"));
        if (config.equals(previous)) return;
        for (Viewer viewer : viewers.values()) {
            Display display = viewer.entities.get(bot);
            if (display != null) {
                display.checkedAt = Long.MIN_VALUE / 2;
            }
        }
    }

    public void activity(java.util.Collection<? extends Player> players, long tick) {
        currentTick = tick;
        online.clear();
        for (Player player : players) {
            UUID id = player.getUniqueId();
            online.add(id);
            Location location = player.getLocation();
            Activity previous = activity.get(id);
            boolean moved = previous == null || !previous.location().getWorld().equals(location.getWorld())
                    || previous.location().distanceSquared(location) > 0.03 * 0.03
                    || Math.abs(angle(previous.location().getYaw() - location.getYaw())) > 0.5
                    || Math.abs(previous.location().getPitch() - location.getPitch()) > 0.5;
            if (moved) {
                activity.put(id, new Activity(location, tick));
            }
        }
        activity.keySet().retainAll(online);
        viewers.keySet().retainAll(online);
    }

    public void update(
            ServerPlayer bot,
            java.util.Collection<? extends Player> players,
            long tick,
            UUID opponent,
            Set<UUID> combatViewers,
            BotVisibilitySettings config,
            String name,
            boolean hasEquipment
    ) {
        UUID botId = bot.getUUID();
        configure(botId, config);
        Player handle = bot.getBukkitEntity();
        for (Player player : players) {
            Viewer viewer = viewers.computeIfAbsent(player.getUniqueId(), id -> new Viewer());
            boolean fighting = player.getUniqueId().equals(opponent)
                    || combatViewers.contains(player.getUniqueId());
            Display display = viewer.entities.get(botId);
            boolean shouldCheckVisibility = fighting || display == null
                    || tick - display.checkedAt >= config.visibilityCheckTicks();
            boolean visible;
            if (shouldCheckVisibility) {
                visible = visible(player, handle, tick, fighting, config);
            } else {
                visible = display.visible;
            }
            if (display != null && tick - display.checkedAt >= config.visibilityCheckTicks()) {
                display.checkedAt = tick;
                display.visible = visible;
            }
            if (!visible) {
                if (display != null) {
                    hide(player, handle, viewer);
                }
                continue;
            }
            if (display == null) {
                display = new Display(tick);
                viewer.entities.put(botId, display);
            }
            if (!display.spawned && tick - display.revealedAt < config.spawnDelayTicks()) continue;
            if (!display.spawned && !fighting) {
                double distance = player.getEyeLocation().distanceSquared(handle.getLocation());
                viewer.pending.put(botId, new Reveal(
                        botId, display, tick, config.maxRevealsPerTick(), distance, player, handle
                ));
                continue;
            }
            viewer.pending.remove(botId);
            if (!display.spawned) {
                show(player, handle, display);
            }
        }
    }

    public void flush(long tick) {
        if (revealTick != tick) {
            revealTick = tick;
            reveals = 0;
        }
        List<Viewer> ordered = new ArrayList<>(viewers.values());
        List<Deque<Reveal>> queues = new ArrayList<>();
        for (Viewer viewer : ordered) {
            if (viewer.revealTick != tick) {
                viewer.revealTick = tick;
                viewer.reveals = 0;
            }
            List<Reveal> pending = new ArrayList<>(viewer.pending.values());
            pending.sort(Comparator.comparingLong((Reveal reveal) -> reveal.display.revealedAt)
                    .thenComparingDouble(Reveal::distance));
            queues.add(new ArrayDeque<>(pending));
            viewer.pending.clear();
        }
        int skipped = 0;
        while (reveals < MAX_REVEALS_PER_TICK && skipped < ordered.size()) {
            int index = Math.floorMod(revealCursor++, ordered.size());
            Viewer viewer = ordered.get(index);
            Reveal reveal = queues.get(index).pollFirst();
            if (reveal == null || viewer.reveals >= reveal.maxPerTick) {
                skipped++;
                continue;
            }
            if (reveal.tick != tick || viewer.entities.get(reveal.bot) != reveal.display
                    || reveal.display.spawned) {
                continue;
            }
            Player player = reveal.player;
            Display display = reveal.display;
            if (player != null && player.isOnline() && reveal.handle != null) {
                show(player, reveal.handle, display);
            }
            viewer.reveals++;
            reveals++;
            skipped = 0;
        }
    }

    private boolean visible(
            Player player,
            Player bot,
            long tick,
            boolean fighting,
            BotVisibilitySettings config
    ) {
        Location location = bot.getLocation();
        if (!player.getWorld().equals(bot.getWorld())) return false;
        if (!player.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        Location eye = player.getEyeLocation();
        double dx = location.getX() - eye.getX();
        double dy = location.getY() + bot.getEyeHeight() - eye.getY();
        double dz = location.getZ() - eye.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > config.viewDistance() * config.viewDistance()) return false;
        if (fighting) return true;
        Activity current = activity.get(player.getUniqueId());
        if (eye.getY() > config.maxViewerY()) return false;
        if (current != null && tick - current.tick() >= (long) (config.idleSeconds() * 20.0D)) {
            return false;
        }
        if (distanceSquared <= config.nearbyDistance() * config.nearbyDistance()) return true;
        double yaw = Math.toRadians(eye.getYaw());
        double pitch = Math.toRadians(eye.getPitch());
        double horizontal = Math.cos(pitch);
        double dot = (-Math.sin(yaw) * horizontal * dx - Math.sin(pitch) * dy
                + Math.cos(yaw) * horizontal * dz) / Math.sqrt(distanceSquared);
        if (dot < Math.cos(Math.toRadians(config.viewAngle()))) return false;
        return player.hasLineOfSight(bot);
    }

    /**
     * 播放机器人动作；服务端会把动画广播给正在追踪它的玩家。
     */
    public void action(ServerPlayer bot, int action) {
        bot.swing(InteractionHand.MAIN_HAND);
    }

    public boolean isVisible(UUID player, UUID bot) {
        Viewer viewer = viewers.get(player);
        Display display = viewer == null ? null : viewer.entities.get(bot);
        return display != null && display.spawned;
    }

    public void destroy(ServerPlayer bot) {
        UUID botId = bot.getUUID();
        Player handle = bot.getBukkitEntity();
        for (Map.Entry<UUID, Viewer> entry : viewers.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !entry.getValue().entities.containsKey(botId)) continue;
            hide(player, handle, entry.getValue());
        }
        configurations.remove(botId);
    }

    public void clear() {
        for (Map.Entry<UUID, Viewer> entry : viewers.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            for (Display display : entry.getValue().entities.values()) {
                display.spawned = false;
            }
        }
        configurations.clear();
        viewers.clear();
        activity.clear();
        online.clear();
        revealTick = Long.MIN_VALUE;
        reveals = revealCursor = 0;
    }

    private void show(Player player, Player bot, Display display) {
        if (display.spawned) return;
        display.spawned = true;
        player.showPlayer(this.plugin, bot);
    }

    /**
     * 隐藏：先清掉待揭示队列，再让服务端停止向该玩家追踪机器人。
     */
    private void hide(Player player, Player bot, Viewer viewer) {
        Display display = viewer.entities.remove(bot.getUniqueId());
        viewer.pending.remove(bot.getUniqueId());
        if (display == null || !display.spawned) return;
        player.hidePlayer(this.plugin, bot);
    }

    private record Reveal(
            UUID bot,
            Display display,
            long tick,
            int maxPerTick,
            double distance,
            Player player,
            Player handle
    ) {
    }

    private static final class Viewer {
        private final Map<UUID, Display> entities = new HashMap<>();
        private final Map<UUID, Reveal> pending = new HashMap<>();
        private long revealTick;
        private int reveals;
    }

    private static final class Display {
        private final long revealedAt;
        private long checkedAt;
        private boolean visible = true;
        private boolean spawned;

        private Display(long tick) {
            this.revealedAt = tick;
            this.checkedAt = tick;
        }
    }

    private record Activity(Location location, long tick) {
    }
}
