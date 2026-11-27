package net.mizukilab.pit.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

final class BundledLobbyChestCleanup {
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

    private BundledLobbyChestCleanup() {
    }

    static void removeFrom(World world, Plugin plugin) {
        NamespacedKey marker = new NamespacedKey(plugin, "bundled_lobby_chest_cleanup_v1");
        var worldData = world.getPersistentDataContainer();
        if (worldData.has(marker) || !matchesBundledLobby(world)) {
            return;
        }

        int removed = 0;
        for (ChestColumn column : OLD_CHESTS) {
            if (!world.isChunkGenerated(column.x() >> 4, column.z() >> 4)) {
                continue;
            }
            for (int y = column.bottomY(); y <= column.topY(); y++) {
                Block block = world.getBlockAt(column.x(), y, column.z());
                if (isOriginalEmptyChest(block, column.facing())) {
                    block.setType(Material.AIR, false);
                    removed++;
                }
            }
        }
        worldData.set(marker, PersistentDataType.BYTE, (byte) 1);
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
