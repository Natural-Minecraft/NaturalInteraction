package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/** Checks if a numeric fact is greater than a threshold. */
public class FactGreaterThanCondition implements Condition {
    private final String factKey;
    private final float threshold;

    public FactGreaterThanCondition(String factKey, float threshold) {
        this.factKey = factKey;
        this.threshold = threshold;
    }

    @Override public String getType() { return "fact_greater_than"; }

    @Override
    public boolean evaluate(Player player, NaturalInteraction plugin) {
        return plugin.getFactsManager().getFloat(player.getUniqueId(), factKey, 0) > threshold;
    }
}
