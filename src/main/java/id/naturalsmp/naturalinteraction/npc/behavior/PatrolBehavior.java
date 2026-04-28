package id.naturalsmp.naturalinteraction.npc.behavior;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;

/**
 * Makes the NPC walk between waypoints in a patrol route.
 */
public class PatrolBehavior implements NPCBehavior {

    private final Location[] waypoints;
    private int currentIndex = 0;
    private boolean forward = true; // false = backward (ping-pong mode)
    private final boolean pingPong;
    private int idleTicks = 0;
    private final int idleTicksPerWaypoint;

    public PatrolBehavior(Location[] waypoints, boolean pingPong, int idleTicksPerWaypoint) {
        this.waypoints = waypoints;
        this.pingPong = pingPong;
        this.idleTicksPerWaypoint = idleTicksPerWaypoint;
    }

    @Override public String getType() { return "patrol"; }
    @Override public int getPriority() { return 1; }

    @Override
    public void onTick(NPC npc) {
        if (waypoints == null || waypoints.length == 0) return;
        if (!npc.isSpawned()) return;

        Location target = waypoints[currentIndex];
        Location npcLoc = npc.getStoredLocation();

        // If close enough to target, idle then advance
        if (npcLoc.distanceSquared(target) < 4.0) { // within 2 blocks
            idleTicks++;
            if (idleTicks >= idleTicksPerWaypoint) {
                idleTicks = 0;
                advanceWaypoint();
            }
            return;
        }

        // Walk to current waypoint
        npc.getNavigator().setTarget(target);
    }

    private void advanceWaypoint() {
        if (pingPong) {
            if (forward) {
                currentIndex++;
                if (currentIndex >= waypoints.length) {
                    currentIndex = waypoints.length - 2;
                    forward = false;
                    if (currentIndex < 0) currentIndex = 0;
                }
            } else {
                currentIndex--;
                if (currentIndex < 0) {
                    currentIndex = 1;
                    forward = true;
                    if (currentIndex >= waypoints.length) currentIndex = 0;
                }
            }
        } else {
            currentIndex = (currentIndex + 1) % waypoints.length;
        }
    }
}
