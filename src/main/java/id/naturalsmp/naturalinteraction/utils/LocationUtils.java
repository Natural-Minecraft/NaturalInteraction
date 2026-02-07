package id.naturalsmp.naturalinteraction.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class LocationUtils {

    /**
     * Find a safe ground location near the origin using raycast.
     * 
     * @param origin Starting location
     * @param radius Search radius
     * @return Safe ground location, or null if none found
     */
    public static Location findSafeGround(Location origin, int radius) {
        // First, try direct raycast downward
        Location direct = raycastDown(origin.clone());
        if (direct != null && isSafeForSpawn(direct)) {
            return direct;
        }

        // Spiral search within radius
        for (int r = 1; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) != r && Math.abs(z) != r)
                        continue;

                    Location test = origin.clone().add(x, 0, z);
                    Location result = raycastDown(test);
                    if (result != null && isSafeForSpawn(result)) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private static Location raycastDown(Location start) {
        Location check = start.clone();
        for (int y = 0; y < 50; y++) {
            check.setY(start.getY() - y);
            Block block = check.getBlock();
            if (block.getType().isSolid() && block.getType() != Material.BARRIER) {
                return block.getLocation().add(0.5, 1, 0.5);
            }
        }
        return null;
    }

    private static boolean isSafeForSpawn(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        if (feet.getType().isSolid() || head.getType().isSolid())
            return false;
        if (ground.getType() == Material.WATER || ground.getType() == Material.LAVA)
            return false;
        return true;
    }

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
        Block down = block.getRelative(BlockFace.DOWN);
        Block up = block.getRelative(BlockFace.UP);

        // Ground must be solid and not liquid
        if (!down.getType().isSolid() || down.isLiquid())
            return false;

        // Feet block must be air-like, not solid, not liquid, and not waterlogged
        if (type.isSolid() || block.isLiquid() || type == Material.WATER)
            return false;

        if (block.getBlockData() instanceof org.bukkit.block.data.Waterlogged) {
            if (((org.bukkit.block.data.Waterlogged) block.getBlockData()).isWaterlogged())
                return false;
        }

        // Head room must also be clear
        if (up.getType().isSolid() || up.isLiquid())
            return false;

        return true;
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
