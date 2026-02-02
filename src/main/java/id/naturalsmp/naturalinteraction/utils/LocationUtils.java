package id.naturalsmp.naturalinteraction.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class LocationUtils {

    /**
     * Finds the nearest shoreline (land next to water) within a radius.
     * 
     * @param center The search origin
     * @param radius The search radius
     * @return The location of the shoreline block, or null if not found.
     */
    public static Location findNearestShoreline(Location center, int radius) {
        Location best = null;
        double minDistance = Double.MAX_VALUE;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Scan a small Y range around player
                for (int y = -5; y <= 5; y++) {
                    Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);

                    if (isStandable(block) && isAdjacentToWater(block)) {
                        double dist = center.distanceSquared(block.getLocation());
                        if (dist < minDistance) {
                            minDistance = dist;
                            best = block.getLocation().add(0.5, 0, 0.5); // Center of block
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean isStandable(Block block) {
        // Must be non-solid/passable block (Air, Grass, etc)
        // AND have a solid block below it
        return !block.getType().isSolid() &&
                !block.isLiquid() &&
                block.getRelative(BlockFace.DOWN).getType().isSolid() &&
                !block.getRelative(BlockFace.DOWN).isLiquid();
    }

    private static boolean isAdjacentToWater(Block block) {
        // Check horizontal neighbors for water
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            if (neighbor.getType() == Material.WATER || neighbor.getType() == Material.SEAGRASS
                    || neighbor.getType() == Material.TALL_SEAGRASS) {
                return true;
            }
        }
        return false;
    }
}
