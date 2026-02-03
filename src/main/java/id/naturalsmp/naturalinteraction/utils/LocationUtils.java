package id.naturalsmp.naturalinteraction.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class LocationUtils {

    public static Location findNearestShoreline(Location center, int radius) {
        ShorelineResult result = findSafeShoreline(center, radius);
        return result != null ? result.landLoc : null;
    }

    public static ShorelineResult findSafeShoreline(Location center, int radius) {
        ShorelineResult best = null;
        double minDistance = Double.MAX_VALUE;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    Block block = center.getWorld().getBlockAt(cx + x, cy + y, cz + z);

                    if (isStandable(block)) {
                        Block water = getAdjacentWater(block);
                        if (water != null) {
                            double dist = center.distanceSquared(block.getLocation());
                            if (dist < minDistance) {
                                minDistance = dist;
                                best = new ShorelineResult(
                                        block.getLocation().add(0.5, 0, 0.5),
                                        water.getLocation().add(0.5, 0, 0.5));
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    public static class ShorelineResult {
        public final Location landLoc;
        public final Location waterLoc;

        public ShorelineResult(Location landLoc, Location waterLoc) {
            this.landLoc = landLoc;
            this.waterLoc = waterLoc;
        }
    }

    private static boolean isStandable(Block block) {
        Material type = block.getType();
        // Standable on solid ground with air/passable above
        return !type.isSolid() &&
                !block.isLiquid() &&
                block.getRelative(BlockFace.DOWN).getType().isSolid() &&
                !block.getRelative(BlockFace.DOWN).isLiquid();
    }

    private static Block getAdjacentWater(Block block) {
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace face : faces) {
            Block neighbor = block.getRelative(face);
            if (neighbor.getType() == Material.WATER) {
                return neighbor;
            }
        }
        return null;
    }
}
