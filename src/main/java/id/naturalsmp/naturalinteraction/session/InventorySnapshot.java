package id.naturalsmp.naturalinteraction.session;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Captures and restores a player's inventory state during a dialogue session.
 * The hotbar is cleared for option items while this snapshot holds the real items.
 */
public class InventorySnapshot {

    private final ItemStack[] contents;

    public InventorySnapshot(Player player) {
        this.contents = player.getInventory().getContents().clone();
    }

    /** Restore the saved inventory contents to the player. */
    public void restore(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(contents);
        player.updateInventory();
    }

    public ItemStack[] getContents() {
        return contents;
    }

    /**
     * Check if the snapshot contains at least {@code amount} of the given item.
     * Supports vanilla material names and ItemsAdder namespaced IDs (namespace:id).
     */
    public boolean hasItem(String itemString, int amount) {
        int count = 0;
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (matchesItem(item, itemString)) count += item.getAmount();
        }
        return count >= amount;
    }

    /** Remove {@code amount} of the given item from the snapshot. */
    public void takeItem(String itemString, int amount) {
        int remaining = amount;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;
            if (!matchesItem(item, itemString)) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                contents[i] = null;
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) break;
        }
    }

    /**
     * Add an item to the snapshot. Returns any overflow that couldn't fit
     * (caller is responsible for dropping it). Returns null if fully added.
     */
    public ItemStack addItem(ItemStack itemToAdd) {
        ItemStack clone = itemToAdd.clone();
        int maxStack = clone.getMaxStackSize();

        // Try to stack with existing similar items
        for (ItemStack item : contents) {
            if (item == null || !item.isSimilar(clone) || item.getAmount() >= maxStack) continue;
            int space = maxStack - item.getAmount();
            if (clone.getAmount() <= space) {
                item.setAmount(item.getAmount() + clone.getAmount());
                return null;
            }
            item.setAmount(maxStack);
            clone.setAmount(clone.getAmount() - space);
        }

        // Find empty slot
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null || contents[i].getType() == Material.AIR) {
                contents[i] = clone;
                return null;
            }
        }

        return clone; // Inventory full — caller should drop
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private boolean matchesItem(ItemStack item, String itemString) {
        if (itemString.contains(":")) {
            try {
                dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.byItemStack(item);
                return cs != null && cs.getNamespacedID().equalsIgnoreCase(itemString);
            } catch (NoClassDefFoundError ignored) {
                return false;
            }
        }
        return item.getType().name().equalsIgnoreCase(itemString);
    }
}
