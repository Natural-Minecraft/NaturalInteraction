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
 * 
 * This listener only works if NaturalDungeon is present.
 */
public class DungeonCompletionListener implements Listener {

    private final NaturalInteraction plugin;

    public DungeonCompletionListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDungeonComplete(Object event) {
        // Cast dynamically to avoid compile-time dependency
        try {
            Class<?> eventClass = Class.forName("id.naturalsmp.naturaldungeon.event.DungeonCompleteEvent");

            if (!eventClass.isInstance(event))
                return;

            String dungeonId = (String) eventClass.getMethod("getDungeonId").invoke(event);
            @SuppressWarnings("unchecked")
            java.util.List<UUID> playerUuids = (java.util.List<UUID>) eventClass.getMethod("getPlayerUuids")
                    .invoke(event);

            // Check config for dungeon -> interaction mapping
            String interactionId = plugin.getConfig().getString("triggers." + dungeonId);

            if (interactionId == null || interactionId.isEmpty()) {
                return; // No mapped interaction
            }

            InteractionManager manager = plugin.getInteractionManager();

            // Start interaction for all participants
            for (UUID uuid : playerUuids) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        manager.startInteraction(player, interactionId);
                    }, 60L); // Delay to let completion GUI close first
                }
            }

            plugin.getLogger().info("[Dungeon-Story] Triggered interaction '" + interactionId
                    + "' for " + playerUuids.size() + " players after dungeon '" + dungeonId + "'");

        } catch (Exception e) {
            // NaturalDungeon not loaded or event class mismatch - ignore silently
        }
    }
}
