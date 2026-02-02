package id.naturalsmp.naturalinteraction.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GUI) {
            GUI gui = (GUI) event.getInventory().getHolder();

            if (!gui.isInteractable()) {
                event.setCancelled(true);
            } else {
                // For interactable GUIs (like ItemRewardGUI), we still want to block
                // some potentially exploitative actions globally.
                switch (event.getAction()) {
                    case COLLECT_TO_CURSOR, MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                        // If clicking in top inventory, block these moves
                        if (event.getClickedInventory() != null
                                && event.getClickedInventory().equals(event.getInventory())) {
                            event.setCancelled(true);
                        }
                    }
                    default -> {
                    }
                }
            }
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GUI) {
            GUI gui = (GUI) event.getInventory().getHolder();
            if (!gui.isInteractable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GUI) {
            ((GUI) event.getInventory().getHolder()).handleClose(event);
        }
    }
}
