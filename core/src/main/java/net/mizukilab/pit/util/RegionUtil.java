package net.mizukilab.pit.util;

import cn.charlotte.pit.ThePit;
import net.mizukilab.pit.config.PitWorldConfig;
import net.mizukilab.pit.util.aabb.AABB;
import org.bukkit.Location;

/**
 * 出生点保护区判定。
 *
 * <p>{@code loc.region.pitA} 与 {@code loc.region.pitB} 围出的 AABB 是出生点保护区（与
 * {@code Utils#isInArena} 的取反语义一致）：箱子、空投等活动物件不能出现在这里，
 * 否则玩家在出生点就能白拿奖励。</p>
 */
public final class RegionUtil {

    private RegionUtil() {
    }

    /** 该位置是否落在出生点保护区内。 */
    public static boolean isInSpawnRegion(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        PitWorldConfig config = ThePit.getInstance().getPitConfig();
        if (config == null) {
            return false;
        }
        Location a = config.getPitLocA();
        Location b = config.getPitLocB();
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(location.getWorld())) {
            return false;
        }
        AABB spawn = new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        AABB point = new AABB(location.getX(), location.getY(), location.getZ(),
                location.getX() + 0.8, location.getY() + 2, location.getZ() + 0.8);
        return spawn.intersectsWith(point);
    }

    /** 该位置是否可以生成玩法物件（保护区以外）。 */
    public static boolean isPlayableLocation(Location location) {
        return !isInSpawnRegion(location);
    }
}
