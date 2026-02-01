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
        
        // Protect bottom row
        if (slot >= 45 && slot < 54) {
            event.setCancelled(true);
        }
        
        // Back Button
        if (slot == 49) {
            saveRewards();
            saving = true; // Prevent double save on close
            new RewardTypeGUI(plugin, player, interaction).open();
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
