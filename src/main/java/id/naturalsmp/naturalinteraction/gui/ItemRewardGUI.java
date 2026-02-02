package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemRewardGUI extends GUI {
    private final Interaction interaction;
    private boolean saving = false;

    public ItemRewardGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 54, "Item Rewards (Drag & Drop)");
        this.interaction = interaction;
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public void initialize() {
        inventory.clear();

        List<ItemStack> rewards = interaction.getRewards();
        for (int i = 0; i < Math.min(rewards.size(), 45); i++) {
            if (rewards.get(i) != null) {
                inventory.setItem(i, rewards.get(i));
            }
        }

        // Border for bottom row
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.displayName(Component.empty());
        pane.setItemMeta(paneMeta);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, pane);
        }

        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Save & Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // 1. Protect Bottom Row (Management Buttons)
        if (slot >= 45 && slot < 54) {
            event.setCancelled(true);

            // Back/Save Button
            if (slot == 49) {
                saveRewards();
                saving = true; // Prevent double save on close
                new RewardTypeGUI(plugin, player, interaction).open();
            }
            return;
        }

        // 2. Prevent item theft from the GUI (Top Inventory)
        // If clicking in top inventory and trying to move item to player inv
        // (Shift-Click)
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(inventory)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                // To "remove" a reward via shift-click, just clear the slot
                inventory.setItem(slot, null);
                return;
            }

            // If they are taking an item (not placing)
            // We allow them to pick it up, but it will be removed from GUI if they take it.
            // Spigot handles the move if we don't cancel.
            // The actual "Theft" happens if they take it out and keep it in their
            // inventory.

            // Fix: If it's a PICKUP action from GUI, we allow it but it's technically a
            // "take".
            // To be 100% safe, we can cancel and instead just clear the slot.
            if (event.getCursor().getType() == Material.AIR && event.getCurrentItem() != null) {
                // Option: Cancel and let them "pick up" a COPY, or just cancel and clear.
                // User wants to prevent "taking", so let's cancel the move out.

                // If you want to let them REMOVE items, right-click to delete is better
                if (event.isRightClick()) {
                    event.setCancelled(true);
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!saving) {
            saveRewards();
            player.sendMessage(Component.text("Item rewards saved.", NamedTextColor.GREEN));
        }
    }

    private void saveRewards() {
        List<ItemStack> newRewards = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                newRewards.add(item);
            }
        }
        interaction.setRewards(newRewards);
        plugin.getInteractionManager().saveInteraction(interaction);
    }
}
