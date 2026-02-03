package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import net.citizensnpcs.api.event.NPCClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EditorListener implements Listener {

    private final NaturalInteraction plugin;

    public EditorListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (StoryWand.isWand(item)) {
            event.setCancelled(true);
            if (event.getClickedBlock() != null) {
                Player player = event.getPlayer();
                String locStr = event.getClickedBlock().getWorld().getName() + ":" +
                        event.getClickedBlock().getX() + "," +
                        event.getClickedBlock().getY() + "," +
                        event.getClickedBlock().getZ();
                player.sendMessage(ChatUtils.toComponent("<green>Selected Location: <yellow>" + locStr));
            }
        }
    }

    @EventHandler
    public void onNPCWand(NPCClickEvent event) {
        Player player = event.getClicker();
        if (StoryWand.isWand(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            player.sendMessage(ChatUtils.toComponent("<green>Selected NPC: <yellow>" + event.getNPC().getName()
                    + " (ID: " + event.getNPC().getId() + ")"));
        }
    }

}
