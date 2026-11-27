package net.mizukilab.pit.config;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public final class BundledLobbyChestCleanup implements Listener {
    // Exact locations from the old bundled lobby, including the two isolated chests.
    private static final ChestColumn[] OLD_CHESTS = {
            new ChestColumn(-20, 5, 117, 122, BlockFace.NORTH),
            new ChestColumn(-15, 18, 114, 119, BlockFace.NORTH),
            new ChestColumn(-12, -3, 115, 115, BlockFace.EAST),
            new ChestColumn(-12, -1, 114, 119, BlockFace.NORTH),
            new ChestColumn(-10, -9, 114, 118, BlockFace.NORTH),
            new ChestColumn(-10, -9, 121, 121, BlockFace.NORTH),
            new ChestColumn(-5, 5, 115, 120, BlockFace.NORTH),
            new ChestColumn(-3, -9, 114, 119, BlockFace.NORTH),
            new ChestColumn(3, 11, 114, 119, BlockFace.NORTH)
    };

    private final Plugin plugin;
    private final NamespacedKey marker;

    private BundledLobbyChestCleanup(Plugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "bundled_lobby_chest_cleanup_v2");
    }

    public static void register(Plugin plugin) {
        var cleanup = new BundledLobbyChestCleanup(plugin);
        Bukkit.getPluginManager().registerEvents(cleanup, plugin);
        Bukkit.getWorlds().forEach(cleanup::removeFrom);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        removeFrom(event.getWorld());
    }

    private void removeFrom(World world) {
        if (world.getEnvironment() != World.Environment.NORMAL || !matchesBundledLobby(world)) {
            return;
        }

        int removed = 0;
        Set<Chunk> scannedChunks = new HashSet<>();
        for (ChestColumn column : OLD_CHESTS) {
            int chunkX = column.x() >> 4;
            int chunkZ = column.z() >> 4;
            if (!world.isChunkGenerated(chunkX, chunkZ)) {
                continue;
            }
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (chunk.getPersistentDataContainer().has(marker)) {
                continue;
            }
            for (int y = column.bottomY(); y <= column.topY(); y++) {
                Block block = world.getBlockAt(column.x(), y, column.z());
                if (isOriginalEmptyChest(block, column.facing())) {
                    block.setType(Material.AIR, false);
                    removed++;
                }
            }
            scannedChunks.add(chunk);
        }
        // Keep the migration marker in the same saved chunk as the blocks it covers.
        for (Chunk chunk : scannedChunks) {
            chunk.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        }
        if (removed > 0) {
            plugin.getLogger().info("[Map] Removed " + removed + " old bundled lobby chests from " + world.getName());
        }
    }

    private static boolean matchesBundledLobby(World world) {
        return world.isChunkGenerated(-1, 1) && world.isChunkGenerated(-2, 0)
                && world.isChunkGenerated(-1, 0)
                && world.getBlockAt(-15, 113, 18).getType() == Material.SLIME_BLOCK
                && world.getBlockAt(-20, 116, 5).getType() == Material.SPRUCE_WOOD
                && world.getBlockAt(-5, 114, 5).getType() == Material.LIGHT_GRAY_CARPET;
    }

    private static boolean isOriginalEmptyChest(Block block, BlockFace facing) {
        if (block.getType() != Material.CHEST
                || !(block.getBlockData() instanceof org.bukkit.block.data.type.Chest data)
                || data.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE
                || data.getFacing() != facing || data.isWaterlogged()) {
            return false;
        }
        return block.getState() instanceof Chest chest && chest.getSnapshotInventory().isEmpty()
                && chest.getLootTable() == null && !chest.isLocked() && chest.customName() == null
                && chest.getPersistentDataContainer().isEmpty();
    }

    private record ChestColumn(int x, int z, int bottomY, int topY, BlockFace facing) {
    }
}
