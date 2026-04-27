package id.naturalsmp.naturalinteraction.manifest.condition;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/** Checks if a player has a specific permission. */
public class PermissionCondition implements Condition {
    private final String permission;

    public PermissionCondition(String permission) { this.permission = permission; }

    @Override public String getType() { return "player_has_permission"; }

    @Override
    public boolean evaluate(Player player, NaturalInteraction plugin) {
        return player.hasPermission(permission);
    }
}
