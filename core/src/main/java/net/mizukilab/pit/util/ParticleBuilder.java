package net.mizukilab.pit.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * 粒子播放工具。
 * <p>
 * 1.21 适配：移除 {@code EnumParticle} 与手写粒子包，改用 Bukkit {@link Particle} API，
 * 由服务端自行处理可见范围与协议版本差异。
 */
public class ParticleBuilder {
    Location location;
    Particle particle;
    float offsetX;
    float offsetY;
    float offsetZ;
    float velocity;
    int count;

    public ParticleBuilder(Location location, Particle particle) {
        this.location = location;
        this.particle = particle;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        this.offsetZ = 0.0f;
        this.velocity = 0.0f;
        this.count = 1;
    }

    public void play() {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, velocity);
    }

    public ParticleBuilder setOffsetX(float offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public ParticleBuilder setOffsetY(float offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public ParticleBuilder setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }

    public ParticleBuilder setVelocity(float velocity) {
        this.velocity = velocity;
        return this;
    }

    public ParticleBuilder setCount(int count) {
        this.count = count;
        return this;
    }

    /**
     * 旧实现用 REDSTONE / SPELL_MOB 的 offset 分量携带 RGB，1.21 改为 DustOptions / EntityEffect 的 data。
     */
    public static void playColored(Location loc, Color color, Particle particle) {
        if (loc.getWorld() == null) {
            return;
        }
        if (particle == Particle.DUST) {
            loc.getWorld().spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 1,
                    new Particle.DustOptions(color, 1.0f));
            return;
        }
        if (particle == Particle.ENTITY_EFFECT) {
            loc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc, 0, 0, 0, 0, 1, color);
            return;
        }
        loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
    }

    public static void playColoredDust(Location loc, Color color) {
        ParticleBuilder.playColored(loc, color, Particle.DUST);
    }

    public static void playColoredSpell(Location loc, Color color) {
        ParticleBuilder.playColored(loc, color, Particle.ENTITY_EFFECT);
    }
}
