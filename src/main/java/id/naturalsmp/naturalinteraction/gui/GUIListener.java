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

            // 1. If not interactable, block everything
            if (!gui.isInteractable()) {
                event.setCancelled(true);
            } else {
                // 2. If interactable (e.g. ItemRewardGUI), we must be VERY careful.
                // We want to allow PLACING items into the top, but block TAKING items from it.

                // Block global actions that move items between inventories
                switch (event.getAction()) {
                    case MOVE_TO_OTHER_INVENTORY -> {
                        // If shift-clicking FROM the custom GUI to player inv, block it.
                        if (event.getClickedInventory() != null
                                && event.getClickedInventory().equals(event.getInventory())) {
                            event.setCancelled(true);
                        }
                        // Shift-clicking FROM player inv TO custom GUI is ALLOWED for interactable GUIs
                    }
                    case COLLECT_TO_CURSOR, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                        // Block these if they involve the top inventory
                        if (event.getClickedInventory() != null
                                && event.getClickedInventory().equals(event.getInventory())) {
                            event.setCancelled(true);
                        }
                    }
                    case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                        // Block picking up from top inventory
                        if (event.getClickedInventory() != null
                                && event.getClickedInventory().equals(event.getInventory())) {
                            event.setCancelled(true);
                        }
                    }
                    default -> {
                    }
                }
            }

            // Allow the GUI to handle the click logic (even if cancelled)
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
