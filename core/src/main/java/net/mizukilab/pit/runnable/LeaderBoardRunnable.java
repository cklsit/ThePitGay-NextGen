package net.mizukilab.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.LeaderBoardEntry;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/3 12:57
 */

public class LeaderBoardRunnable extends BukkitRunnable {

    @SneakyThrows
    @Override
    public void run() {

        updateLeaderboardData();
    }

    public static void updateLeaderboardData() {
        // 1.21：排行榜改为读取全部档案后在内存排序（Mongo/SQLite 双后端统一）
        long cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        List<cn.charlotte.pit.data.PlayerProfile> profiles =
                ThePit.getInstance().getMongoDB().getProfiles().findAll();
        profiles.removeIf(p -> p.getLastLogoutTime() < cutoff);

        profiles.sort((a, b) -> Double.compare(b.getExperience(), a.getExperience()));

        List<LeaderBoardEntry> entries = new ObjectArrayList<>();
        int i = 1;
        for (cn.charlotte.pit.data.PlayerProfile profile : profiles) {
            try {
                entries.add(new LeaderBoardEntry(
                        profile.getPlayerName(),
                        profile.getPlayerUuid(),
                        i,
                        profile.getExperience(),
                        profile.getPrestige()));
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LeaderBoardEntry.setLeaderBoardEntries(entries);
    }
}
