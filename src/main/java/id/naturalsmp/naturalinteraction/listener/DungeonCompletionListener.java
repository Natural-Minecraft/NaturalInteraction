package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listens for DungeonCompleteEvent from NaturalDungeon
 * and starts mapped interactions for all participants.
 */
public class DungeonCompletionListener implements Listener {

    private final NaturalInteraction plugin;

    public DungeonCompletionListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDungeonComplete(id.naturalsmp.naturaldungeon.event.DungeonCompleteEvent event) {
        String dungeonId = event.getDungeonId();

        // Check config for dungeon -> interaction mapping
        String interactionId = plugin.getConfig().getString("triggers." + dungeonId);

        if (interactionId == null || interactionId.isEmpty()) {
            return; // No mapped interaction
        }

        InteractionManager manager = plugin.getInteractionManager();

        // Start interaction for all participants
        for (UUID uuid : event.getPlayerUuids()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    manager.startInteraction(player, interactionId);
                }, 60L); // Delay to let completion GUI close first
            }
        }

        plugin.getLogger().info("[Dungeon-Story] Triggered interaction '" + interactionId
                + "' for " + event.getPlayerUuids().size() + " players after dungeon '" + dungeonId + "'");
    }
}
