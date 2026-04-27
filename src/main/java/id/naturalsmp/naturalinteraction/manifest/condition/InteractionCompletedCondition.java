package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/** Checks if a player has completed a specific interaction. */
public class InteractionCompletedCondition implements Condition {
    private final String interactionId;

    public InteractionCompletedCondition(String interactionId) {
        this.interactionId = interactionId;
    }

    @Override public String getType() { return "interaction_completed"; }

    @Override
    public boolean evaluate(Player player, NaturalInteraction plugin) {
        return plugin.getFactsManager().hasCompleted(player.getUniqueId(), interactionId);
    }
}
