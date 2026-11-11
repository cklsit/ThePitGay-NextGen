package net.mizukilab.pit.util;

import cn.charlotte.pit.ThePit;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@UtilityClass
public class Einstein {
    Vector vector = new Vector(0,0,0);
    public double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }
    public float clampf(float value, float lowerBound, float upperBound) {
        if (value < lowerBound) {
            return lowerBound;
        } else {
            return Math.min(value, upperBound);
        }
    }
    public int sum(Object2IntMap map){
        int sum = 0;
        IntIterator intIterator = map.values().intIterator();
        while(intIterator.hasNext()){
            sum += intIterator.nextInt();
        }
        return sum;
    }
    public int clampi(int value, int lowerBound, int upperBound) {
        if (value < lowerBound) {
            return lowerBound;
        } else {
            return Math.min(value, upperBound);
        }
    }
    public void push0Velocity(Player player) {
        player.setVelocity(vector);
    }

    Vector vector_0 = new Vector(0,0.05,0);
    public void push0_1Velocity(Player player) {
        player.setVelocity(vector_0);
    }
    public void noVelocity(Player player){
        // 1.21：直接清零服务端速度即可；旧的 velocityChanged/motX 字段已随 NMS Entity 一起移除
        player.setVelocity(vector);
    }
    public void flushPos(Player player) {
        if (Bukkit.isPrimaryThread()) {
            player.teleport(player.getLocation());
        } else {
            Bukkit.getScheduler().runTask(ThePit.getInstance(),() -> flushPos(player));
        }
    }
}
