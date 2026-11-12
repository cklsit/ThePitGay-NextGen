package net.mizukilab.pit.handlers;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.LeaderBoardEntry;
import cn.charlotte.pit.data.PlayerProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.SneakyThrows;
import net.mizukilab.pit.util.rank.RankUtil;
import nya.Skip;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/3 12:57
 */
@Skip
public class LeaderBoardRunnable extends BukkitRunnable {

    private static final long ACTIVE_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000;

    @SneakyThrows
    @Override
    public void run() {

        updateLeaderboardData();
    }

    /**
     * 排行榜统计。
     *
     * <p>原实现直接读 Mongo 集合；storage.type=sqlite 时没有原始驱动可用，
     * 这里统一走 DocumentCollection，按 totalExp 降序。</p>
     */
    public static void updateLeaderboardData() {
        List<PlayerProfile> profiles = new ArrayList<>(ThePit.getInstance()
                .getMongoDB()
                .getProfiles()
                .findAll());
        long activeSince = System.currentTimeMillis() - ACTIVE_WINDOW_MILLIS;
        profiles.removeIf(profile -> profile == null
                || profile.getLastLogoutTime() < activeSince
                || profile.getPlayerUuid() == null);
        profiles.sort(Comparator.comparingDouble(PlayerProfile::getTotalExp).reversed());

        Object2ObjectLinkedOpenHashMap<UUID, LeaderBoardEntry> entries = new Object2ObjectLinkedOpenHashMap<>();
        int rank = 1;
        for (PlayerProfile profile : profiles) {
            UUID uuid = profile.getPlayerUuid();
            entries.putAndMoveToLast(uuid, new LeaderBoardEntry(
                    RankUtil.getPlayerRealColoredName(uuid), uuid, rank,
                    profile.getExperience(), profile.getPrestige()));
            rank++;
        }
        LeaderBoardEntry.setLeaderBoardEntries(entries);
    }
}
