package id.naturalsmp.naturalinteraction.npc.behavior;

import org.bukkit.entity.Player;
import net.citizensnpcs.api.npc.NPC;

/**
 * A behavior that an NPC can perform.
 * Behaviors run as part of the NPC's behavior tree.
 */
public interface NPCBehavior {

    /** Unique type identifier for serialization. */
    String getType();

    /** Initialize the behavior. Called once when the NPC spawns. */
    default void onStart(NPC npc) {}

    /** Called every tick while this behavior is active. */
    void onTick(NPC npc);

    /** Called when a player interacts with this NPC while behavior is active. */
    default void onInteract(NPC npc, Player player) {}

    /** Called when behavior stops. */
    default void onStop(NPC npc) {}

    /** Priority (higher = more priority). Used for behavior switching. */
    default int getPriority() { return 0; }
}
