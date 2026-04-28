package id.naturalsmp.naturalinteraction.npc.behavior;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Makes the NPC look at the nearest player within range.
 * If no player is nearby, NPC looks at its default direction.
 */
public class LookAtPlayerBehavior implements NPCBehavior {

    private final double range;

    public LookAtPlayerBehavior(double range) {
        this.range = range;
    }

    @Override public String getType() { return "look_at_player"; }
    @Override public int getPriority() { return 2; }

    @Override
    public void onTick(NPC npc) {
        if (!npc.isSpawned() || npc.getEntity() == null) return;
        Location npcLoc = npc.getStoredLocation();

        Player nearest = null;
        double nearestDist = range * range;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(npcLoc.getWorld())) continue;
            double dist = player.getLocation().distanceSquared(npcLoc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }

        if (nearest != null) {
            npc.faceLocation(nearest.getEyeLocation());
        }
    }
}
