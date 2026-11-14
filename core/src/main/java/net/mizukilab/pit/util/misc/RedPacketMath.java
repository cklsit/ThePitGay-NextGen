package net.mizukilab.pit.util.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 红包金额分配：按距离加权（越近权重越高），保证单人时能拿到全部金额。
 */
public final class RedPacketMath {

    private RedPacketMath() {
    }

    /**
     * @param money     红包总金额
     * @param distances 玩家 UUID -> 与爆炸点的距离
     * @return 玩家 UUID -> 分得金额
     */
    public static Map<UUID, Integer> distribute(int money, Map<UUID, Double> distances) {
        Map<UUID, Integer> result = new HashMap<>();
        if (money <= 0 || distances == null || distances.isEmpty()) {
            return result;
        }
        double maxDistance = 0.0;
        for (double distance : distances.values()) {
            maxDistance = Math.max(maxDistance, distance);
        }
        Map<UUID, Double> weights = new HashMap<>();
        double weightSum = 0.0;
        for (Map.Entry<UUID, Double> entry : distances.entrySet()) {
            // 最近的人权重最高（maxDistance - distance + 1），单人时权重为 1
            double weight = (maxDistance - entry.getValue()) + 1.0;
            weights.put(entry.getKey(), weight);
            weightSum += weight;
        }
        for (Map.Entry<UUID, Double> entry : weights.entrySet()) {
            double share = weightSum <= 0.0 ? 1.0 / distances.size() : entry.getValue() / weightSum;
            result.put(entry.getKey(), (int) Math.floor(money * share));
        }
        return result;
    }
}
