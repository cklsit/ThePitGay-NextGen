import io

p = 'core/src/main/java/net/mizukilab/pit/events/impl/major/RagePitEvent.java'
t = io.open(p, encoding='utf-8').read()

for line in [
    "import com.boydti.fawe.FaweAPI;\n",
    "import com.boydti.fawe.util.TaskManager;\n",
    "import com.sk89q.worldedit.BlockVector;\n",
    "import com.sk89q.worldedit.EditSession;\n",
    "import com.sk89q.worldedit.WorldEdit;\n",
    "import com.sk89q.worldedit.blocks.BaseBlock;\n",
    "import com.sk89q.worldedit.blocks.BlockID;\n",
    "import com.sk89q.worldedit.bukkit.BukkitWorld;\n",
    "import net.mizukilab.pit.util.worldedit.JustAirBlockPattern;\n",
]:
    t = t.replace(line, "")

t = t.replace("    private EditSession session;",
              "    private static final java.util.Map<org.bukkit.Location, org.bukkit.block.data.BlockData> ORIGINAL_BLOCKS = new java.util.HashMap<>();")

old_build = """            Location middle = ThePit.getInstance().getPitConfig().getRagePitMiddle();
            BlockVector vector = new BlockVector(middle.getBlockX(), middle.getBlockY(), middle.getBlockZ());
            BukkitWorld world = new BukkitWorld(Bukkit.getWorlds().get(0));
            try {
                session = FaweAPI.getEditSessionBuilder(world).build();


                Runnable runnable1 = () -> {
                    session.makeCylinder(vector, new JustAirBlockPattern(new BaseBlock(BlockID.GLASS)), ThePit.getInstance().getPitConfig().getRagePitRadius(), ThePit.getInstance().getPitConfig().getRagePitHeight(), false);
                    session.flushQueue();
                };

                TaskManager.IMP.async(runnable1);
            } catch (Throwable e){
                session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, Integer.MAX_VALUE);

                Runnable runnable1 = () -> {
                    session.makeCylinder(vector, new JustAirBlockPattern(new BaseBlock(BlockID.GLASS)), ThePit.getInstance().getPitConfig().getRagePitRadius(), ThePit.getInstance().getPitConfig().getRagePitHeight(), false);
                    session.flushQueue();
                };
                Bukkit.getScheduler().runTask(ThePit.getInstance(), runnable1);
            }
"""

new_build = """            Location middle = ThePit.getInstance().getPitConfig().getRagePitMiddle();
            final int cx = middle.getBlockX();
            final int cy = middle.getBlockY();
            final int cz = middle.getBlockZ();
            final int radius = ThePit.getInstance().getPitConfig().getRagePitRadius();
            final int height = ThePit.getInstance().getPitConfig().getRagePitHeight();
            final org.bukkit.World bukkitWorld = middle.getWorld() == null ? Bukkit.getWorlds().get(0) : middle.getWorld();

            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                ORIGINAL_BLOCKS.clear();
                int radiusSquared = radius * radius;
                for (int x = cx - radius; x <= cx + radius; x++) {
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > radiusSquared) {
                            continue;
                        }
                        for (int y = cy; y < cy + height; y++) {
                            org.bukkit.block.Block block = bukkitWorld.getBlockAt(x, y, z);
                            if (block.getType() == Material.AIR) {
                                ORIGINAL_BLOCKS.put(block.getLocation(), block.getBlockData());
                                block.setType(Material.GLASS, false);
                            }
                        }
                    }
                }
            });
"""

assert old_build in t, 'build pattern missing'
t = t.replace(old_build, new_build)

old_undo = """        try{
        FaweAPI.getTaskManager().async(() -> {
            session.undo(session);
            session.flushQueue();
        });
        } catch (Throwable e){
            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                session.undo(session);
                session.flushQueue();
            });
        }
        /* TaskManager.IMP.async(() -> {
            session.undo(session);
            session.flushQueue();
        }); */"""

new_undo = """        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            for (java.util.Map.Entry<org.bukkit.Location, org.bukkit.block.data.BlockData> entry : ORIGINAL_BLOCKS.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue(), false);
            }
            ORIGINAL_BLOCKS.clear();
        });"""

assert old_undo in t, 'undo pattern missing'
t = t.replace(old_undo, new_undo)

io.open(p, 'w', encoding='utf-8').write(t)
print('ragepit patched')
