/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016 Jaxon A Brown
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 *  persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 *  OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.mizukilab.pit.util.beam.beam;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * 两点之间的光束。
 *
 * <p>原实现依赖 ProtocolLib 伪造守卫者/鱿鱼实体，未安装 ProtocolLib 时会导致类加载失败；
 * 现在改用 Bukkit 原生粒子绘制，零外部依赖。</p>
 *
 * @author Jaxon A Brown
 */
public class LocationTargetBeam {

    /** 每米采样的粒子数，越大越密。 */
    private static final double PARTICLES_PER_BLOCK = 2.0;
    private static final int MAX_PARTICLES = 120;

    private Location startingPosition;
    private Location endingPosition;

    public LocationTargetBeam(Location startingPosition, Location endingPosition) {
        Preconditions.checkNotNull(startingPosition, "startingPosition cannot be null");
        Preconditions.checkNotNull(endingPosition, "endingPosition cannot be null");
        Preconditions.checkState(startingPosition.getWorld().equals(endingPosition.getWorld()),
                "startingPosition and endingPosition must be in the same world");
        this.startingPosition = startingPosition.clone();
        this.endingPosition = endingPosition.clone();
    }

    /** 向该玩家显示光束（粒子即时绘制，无需后续维护）。 */
    public void start(Player player) {
        draw(player);
    }

    public void setStartingPosition(Player player, Location location) {
        this.startingPosition = location.clone();
        draw(player);
    }

    public void setEndingPosition(Player player, Location location) {
        this.endingPosition = location.clone();
        draw(player);
    }

    /** 当前是按帧重绘的粒子，没有客户端实体需要清理。 */
    public void cleanup(Player player) {
        // no-op
    }

    private void draw(Player player) {
        if (player == null || !player.isOnline()) return;
        Location from = startingPosition;
        Location to = endingPosition;
        if (from.getWorld() == null || !from.getWorld().equals(player.getWorld())) return;

        double distance = from.distance(to);
        int count = (int) Math.min(MAX_PARTICLES, Math.max(2, Math.ceil(distance * PARTICLES_PER_BLOCK)));
        double stepX = (to.getX() - from.getX()) / count;
        double stepY = (to.getY() - from.getY()) / count;
        double stepZ = (to.getZ() - from.getZ()) / count;

        for (int i = 0; i <= count; i++) {
            player.spawnParticle(Particle.DUST,
                    from.getX() + stepX * i,
                    from.getY() + stepY * i + 1.0,
                    from.getZ() + stepZ * i,
                    1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 200, 255), 1.0f));
        }
    }
}
