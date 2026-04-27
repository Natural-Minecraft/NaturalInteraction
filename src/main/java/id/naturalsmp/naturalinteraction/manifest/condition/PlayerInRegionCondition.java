package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/**
 * Checks if a player is within a cuboid region defined by two corners.
 *
 * JSON: { "type": "player_in_region", "world": "world", "x1":0, "y1":60, "z1":0, "x2":100, "y2":80, "z2":100 }
 */
public class PlayerInRegionCondition implements Condition {

    private final String world;
    private final double x1, y1, z1, x2, y2, z2;

    public PlayerInRegionCondition(String world, double x1, double y1, double z1,
                                    double x2, double y2, double z2) {
        this.world = world;
        this.x1 = Math.min(x1, x2); this.y1 = Math.min(y1, y2); this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2); this.y2 = Math.max(y1, y2); this.z2 = Math.max(z1, z2);
    }

    @Override
    public String getType() { return "player_in_region"; }

    @Override
    public boolean evaluate(Player player, NaturalInteraction plugin) {
        if (!player.getWorld().getName().equalsIgnoreCase(world)) return false;
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }
}
