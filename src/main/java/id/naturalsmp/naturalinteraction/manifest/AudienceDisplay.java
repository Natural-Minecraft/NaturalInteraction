package id.naturalsmp.naturalinteraction.manifest;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manifest.condition.Condition;
import id.naturalsmp.naturalinteraction.manifest.display.ManifestDisplay;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * AudienceDisplay is a manifest entry that:
 *  1. Has a condition — determines who is "in the audience"
 *  2. Has a display — what to show to players in the audience
 *  3. Has optional children — nested filters/displays
 *
 * When a player is in the audience (condition == true), the display is shown.
 * When they leave (condition == false), the display is hidden.
 */
public class AudienceDisplay implements ManifestEntry {

    private final String id;
    private final Condition condition;          // null = always true (root)
    private final ManifestDisplay display;      // null = filter-only (no display)
    private final List<AudienceDisplay> children;

    // Track who is currently "in" the audience
    private final Set<UUID> audience = new HashSet<>();

    public AudienceDisplay(String id, Condition condition, ManifestDisplay display,
                           List<AudienceDisplay> children) {
        this.id = id;
        this.condition = condition;
        this.display = display;
        this.children = children != null ? children : List.of();
    }

    @Override public String getId()   { return id; }
    @Override public String getType() { return display != null ? "audience_display" : "audience_filter"; }

    /**
     * Evaluate this entry for a player.
     * Called every tick by ManifestTicker.
     *
     * @return true if the player is in this audience
     */
    public boolean tick(Player player, NaturalInteraction plugin) {
        boolean allowed = condition == null || condition.evaluate(player, plugin);
        UUID uuid = player.getUniqueId();

        if (allowed) {
            // Player enters the audience
            if (audience.add(uuid) && display != null) {
                display.show(player, plugin);  // First time entering → show
            } else if (display != null) {
                // Already in audience → re-show (for ActionBar-type displays that need refresh)
                display.show(player, plugin);
            }

            // Tick children
            for (AudienceDisplay child : children) {
                child.tick(player, plugin);
            }
        } else {
            // Player leaves the audience
            if (audience.remove(uuid)) {
                if (display != null) display.hide(player, plugin);
                // Also remove from all children
                for (AudienceDisplay child : children) {
                    child.removePlayer(player, plugin);
                }
            }
        }

        return allowed;
    }

    /**
     * Force remove a player from this audience and all children.
     * Called when player disconnects or parent filter removes them.
     */
    public void removePlayer(Player player, NaturalInteraction plugin) {
        if (audience.remove(player.getUniqueId())) {
            if (display != null) display.hide(player, plugin);
        }
        for (AudienceDisplay child : children) {
            child.removePlayer(player, plugin);
        }
    }

    /**
     * Cleanup all audiences (plugin disable).
     */
    public void cleanup(NaturalInteraction plugin) {
        for (UUID uuid : new ArrayList<>(audience)) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && display != null) {
                display.hide(p, plugin);
            }
        }
        audience.clear();
        for (AudienceDisplay child : children) {
            child.cleanup(plugin);
        }
    }

    public Set<UUID> getAudience()              { return Collections.unmodifiableSet(audience); }
    public Condition getCondition()             { return condition; }
    public ManifestDisplay getDisplay()         { return display; }
    public List<AudienceDisplay> getChildren()  { return children; }
}
