import io

p = 'core/src/main/java/net/mizukilab/pit/events/impl/CakeEvent.java'
t = io.open(p, encoding='utf-8').read()

t = t.replace("""import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
""", "")

t = t.replace("""    private static CuboidRegion[] regions;
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    private EditSession session;
    private AABB alignedBB;""", """    private static java.util.List<org.bukkit.Location[]> zones;
    private static final java.util.Map<org.bukkit.Location, org.bukkit.block.data.BlockData> ORIGINAL_BLOCKS = new java.util.HashMap<>();
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");
    private final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    private AABB alignedBB;""")

t = t.replace("""    public CakeEvent() {
        if (regions == null) {
            PitWorldConfig config = ThePit.getInstance().getPitConfig();
            regions = new CuboidRegion[]{
                    new CuboidRegion(BukkitUtil.toVector(config.getCakeZoneAPosA()), BukkitUtil.toVector(config.getCakeZoneAPosB())),
                    new CuboidRegion(BukkitUtil.toVector(config.getCakeZoneBPosA()), BukkitUtil.toVector(config.getCakeZoneBPosB())),
                    new CuboidRegion(BukkitUtil.toVector(config.getCakeZoneCPosA()), BukkitUtil.toVector(config.getCakeZoneCPosB())),
                    new CuboidRegion(BukkitUtil.toVector(config.getCakeZoneDPosA()), BukkitUtil.toVector(config.getCakeZoneDPosB()))
            };
        }
    }""", """    public CakeEvent() {
        if (zones == null) {
            PitWorldConfig config = ThePit.getInstance().getPitConfig();
            zones = new java.util.ArrayList<>();
            zones.add(new org.bukkit.Location[]{config.getCakeZoneAPosA(), config.getCakeZoneAPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneBPosA(), config.getCakeZoneBPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneCPosA(), config.getCakeZoneCPosB()});
            zones.add(new org.bukkit.Location[]{config.getCakeZoneDPosA(), config.getCakeZoneDPosB()});
        }
    }""")

old_active = """        final CuboidRegion region = regions[RandomUtil.random.nextInt(regions.length)];
        final Vector pos1 = region.getPos1();
        final Vector pos2 = region.getPos2();
        this.alignedBB = new AABB(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());

        TaskManager.IMP.async(() -> {
            BukkitWorld world = new BukkitWorld(Bukkit.getWorlds().get(0));
            this.session = FaweAPI.getEditSessionBuilder(world).build();

            final RandomPattern pattern = new RandomPattern();
            pattern.add(new BaseBlock(BlockID.CAKE_BLOCK), 0.94);
            pattern.add(new BaseBlock(BlockID.STAINED_CLAY, 2), 0.03);
            pattern.add(new BaseBlock(BlockID.STAINED_CLAY, 15), 0.03);

            session.setBlocks(region, pattern);

            session.flushQueue();
        });"""

new_active = """        final org.bukkit.Location[] zone = zones.get(RandomUtil.random.nextInt(zones.size()));
        final int minX = Math.min(zone[0].getBlockX(), zone[1].getBlockX());
        final int minY = Math.min(zone[0].getBlockY(), zone[1].getBlockY());
        final int minZ = Math.min(zone[0].getBlockZ(), zone[1].getBlockZ());
        final int maxX = Math.max(zone[0].getBlockX(), zone[1].getBlockX());
        final int maxY = Math.max(zone[0].getBlockY(), zone[1].getBlockY());
        final int maxZ = Math.max(zone[0].getBlockZ(), zone[1].getBlockZ());
        this.alignedBB = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        org.bukkit.World bukkitWorld = Bukkit.getWorlds().get(0);
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            ORIGINAL_BLOCKS.clear();
            java.util.Random random = new java.util.Random();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        org.bukkit.block.Block block = bukkitWorld.getBlockAt(x, y, z);
                        Material target;
                        double roll = random.nextDouble();
                        if (roll < 0.94) {
                            target = Material.CAKE;
                        } else if (roll < 0.97) {
                            target = Material.RED_TERRACOTTA;
                        } else {
                            target = Material.BLACK_TERRACOTTA;
                        }
                        if (block.getType() != target) {
                            ORIGINAL_BLOCKS.put(block.getLocation(), block.getBlockData());
                            block.setType(target, false);
                        }
                    }
                }
            }
        });"""

assert old_active in t, 'active pattern missing'
t = t.replace(old_active, new_active)

old_inactive = """        TaskManager.IMP.async(() -> {
            this.session.undo(session);
            this.session.flushQueue();
        });"""

new_inactive = """        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            for (java.util.Map.Entry<org.bukkit.Location, org.bukkit.block.data.BlockData> entry : ORIGINAL_BLOCKS.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue(), false);
            }
            ORIGINAL_BLOCKS.clear();
        });"""

assert old_inactive in t, 'inactive pattern missing'
t = t.replace(old_inactive, new_inactive)

io.open(p, 'w', encoding='utf-8').write(t)
print('cake patched')
