package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/** Checks if a fact equals an expected value. */
public class FactEqualsCondition implements Condition {
    private final String factKey;
    private final String expectedValue;

    public FactEqualsCondition(String factKey, String expectedValue) {
        this.factKey = factKey;
        this.expectedValue = expectedValue;
    }

    @Override public String getType() { return "fact_equals"; }

    @Override
    public boolean evaluate(Player player, NaturalInteraction plugin) {
        String actual = plugin.getFactsManager().getString(player.getUniqueId(), factKey, "");
        return expectedValue.equalsIgnoreCase(actual);
    }
}
