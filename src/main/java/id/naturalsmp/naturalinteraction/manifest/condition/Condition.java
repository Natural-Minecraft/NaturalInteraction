package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/**
 * A condition that can be evaluated for a player.
 * Used by AudienceFilter to decide if a player should be in its audience.
 */
public interface Condition {

    /** Type identifier for serialization. */
    String getType();

    /** Evaluate the condition for the given player. */
    boolean evaluate(Player player, NaturalInteraction plugin);
}
