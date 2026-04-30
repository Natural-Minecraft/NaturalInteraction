package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class QuestObjectiveListener implements Listener {
    private final NaturalInteraction plugin;

    public QuestObjectiveListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.hasBlock()) return;

        Player player = e.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL) {
            String activeQuest = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
            if ("tutorial".equals(activeQuest)) {
                String stage = plugin.getQuestManager().getQuestStage(player.getUniqueId(), "tutorial");
                if ("claim_land".equals(stage)) {
                    // Check if they are clicking a block (simulating a claim point)
                    // We will wait 1 second to let the claim plugin process it, then mark as complete
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Mark quest as completed
                        plugin.getQuestManager().setQuestState(player.getUniqueId(), "tutorial", "COMPLETED");
                        plugin.getQuestManager().clearActiveQuest(player.getUniqueId());
                        if (plugin.getQuestOverlay() != null) plugin.getQuestOverlay().updateOverlay(player);
                        
                        player.showTitle(net.kyori.adventure.title.Title.title(
                            id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&6&lTutorial Selesai!"),
                            id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&eSelamat datang di NaturalSMP"),
                            net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(500), 
                                java.time.Duration.ofMillis(3000), 
                                java.time.Duration.ofMillis(1000)
                            )
                        ));
                        player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&a⭐ Quest Selesai: &ePerjalanan Dimulai"));
                        
                        // Give some basic reward
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "eco give " + player.getName() + " 1000");
                    }, 40L); // Wait 2 seconds (40 ticks)
                }
            }
        }
    }
}
