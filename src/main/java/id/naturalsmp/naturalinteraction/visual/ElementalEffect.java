package id.naturalsmp.naturalinteraction.visual;

import org.bukkit.Location;

/**
 * Base interface for NPC elemental visual effects.
 */
public interface ElementalEffect {

    /**
     * Render the visual effect at the given NPC location.
     * Called every 2 ticks from the manager's repeating task.
     * @param npcLocation the current location of the NPC
     * @param tick the current tick counter (increments by 1 each call)
     */
    void render(Location npcLocation, long tick);

    /**
     * Get the base location where NPC should stand.
     * For floating NPCs (Wind), returns a location offset above ground.
     * For others, returns the same location.
     */
    default Location getAdjustedNPCLocation(Location base, long tick) {
        return base;
    }

    /**
     * Called when this effect is being stopped/cleaned up.
     */
    default void cleanup() {}
}
