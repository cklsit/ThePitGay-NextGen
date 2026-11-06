import re


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, t):
    open(p, 'w', encoding='utf-8').write(t)


# ---------- PlayerMoveHandler：粒子包 -> Paper 的单玩家粒子 API ----------
p = 'base/src/main/java/net/mizukilab/pit/movement/PlayerMoveHandler.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;\n", "")

t = t.replace("""            PacketPlayOutWorldParticles explosionPacket = new PacketPlayOutWorldParticles(
                    Particle.EXPLOSION, true,
                    (float) to.getX(), (float) to.getY(), (float) to.getZ(),
                    0.3F, 0.3F, 0.3F, 0, 8
            );
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(explosionPacket);""",
              """            player.spawnParticle(Particle.EXPLOSION, to, 8, 0.3, 0.3, 0.3, 0);""")

t = t.replace("""                            PacketPlayOutWorldParticles trailPacket = new PacketPlayOutWorldParticles(
                                    particleType, true,
                                    (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                                    0.2F, 0.2F, 0.2F, 0, isDescending ? 1 : 2
                            );
                            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(trailPacket);""",
              """                            player.spawnParticle(particleType, loc, isDescending ? 1 : 2, 0.2, 0.2, 0.2, 0);""")

t = t.replace("""                            PacketPlayOutWorldParticles landingPacket = new PacketPlayOutWorldParticles(
                                    Particle.CLOUD, true,
                                    (float) safeLandingLoc.getX(), (float) safeLandingLoc.getY(), (float) safeLandingLoc.getZ(),
                                    0.5F, 0.1F, 0.5F, 0, 8
                            );
                            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(landingPacket);""",
              """                            player.spawnParticle(Particle.CLOUD, safeLandingLoc, 8, 0.5, 0.1, 0.5, 0);""")
write(p, t)

# ---------- FishingHandler：真实掉落物实体 ----------
p = 'base/src/main/java/net/mizukilab/pit/fishing/FishingHandler.java'
t = read(p)
t = t.replace("import net.minecraft.server.v1_8_R3.EntityItem;\n", "")
t = t.replace("import net.minecraft.server.v1_8_R3.WorldServer;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;\n", "")
t = t.replace("import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;\n", "")
t = t.replace("""    private EntityItem generateEntity(Player player, Location dropLocation, ItemStack item) {
        WorldServer worldServer = ((CraftWorld) dropLocation.getWorld()).getHandle();
        EntityItem entityItem = new EntityItem(worldServer, dropLocation.getX(), dropLocation.getY(), dropLocation.getZ(), PublicUtil.toNMStackQuick(item));
        worldServer.addEntity(entityItem);

        double d5 = player.getLocation().getX() - dropLocation.getX();
        double d6 = player.getLocation().getY() - dropLocation.getY();
        double d7 = player.getLocation().getZ() - dropLocation.getZ();
        double d8 = MathHelper.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
        double d9 = 0.1D;

        entityItem.motX = d5 * d9;
        entityItem.motY = d6 * d9 + (double) MathHelper.sqrt(d8) * 0.08D;
        entityItem.motZ = d7 * d9;

        return entityItem;
    }""",
              """    private org.bukkit.entity.Item generateEntity(Player player, Location dropLocation, ItemStack item) {
        // 1.21：直接生成 Bukkit 掉落物，速度按原公式换算
        org.bukkit.entity.Item entityItem = dropLocation.getWorld().dropItem(dropLocation, item);
        entityItem.setPickupDelay(10);

        double d5 = player.getLocation().getX() - dropLocation.getX();
        double d6 = player.getLocation().getY() - dropLocation.getY();
        double d7 = player.getLocation().getZ() - dropLocation.getZ();
        double d8 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
        double d9 = 0.1D;

        org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(
                d5 * d9,
                d6 * d9 + Math.sqrt(d8) * 0.08D,
                d7 * d9);
        entityItem.setVelocity(velocity);

        return entityItem;
    }""")
write(p, t)

print('batch4 ok')
