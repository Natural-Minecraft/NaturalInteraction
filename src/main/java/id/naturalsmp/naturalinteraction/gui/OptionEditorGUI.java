package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.Option;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OptionEditorGUI extends GUI {
    private final Interaction interaction;
    private final DialogueNode node;

    public OptionEditorGUI(NaturalInteraction plugin, Player player, Interaction interaction, DialogueNode node) {
        super(plugin, player, 54, "Options: " + node.getId());
        this.interaction = interaction;
        this.node = node;
    }

    @Override
    public void initialize() {
        inventory.clear();

        // List Options
        for (int i = 0; i < node.getOptions().size(); i++) {
            Option option = node.getOptions().get(i);
            ItemStack item = new ItemStack(Material.OAK_SIGN);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(option.getText(), NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Target: " + option.getTargetNodeId(), NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Right-Click to Delete", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Add Option Button
        ItemStack add = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = add.getItemMeta();
        addMeta.displayName(Component.text("Add Option", NamedTextColor.GREEN));
        add.setItemMeta(addMeta);
        inventory.setItem(49, add);
        
        // Auto-Link (Default Next Node if no options)
        ItemStack autoLink = new ItemStack(Material.COMPASS);
        ItemMeta autoMeta = autoLink.getItemMeta();
        autoMeta.displayName(Component.text("Set Auto-Next Node", NamedTextColor.AQUA));
        List<Component> autoLore = new ArrayList<>();
        autoLore.add(Component.text("Current: " + (node.getNextNodeId() == null ? "None" : node.getNextNodeId()), NamedTextColor.GRAY));
        autoMeta.lore(autoLore);
        autoLink.setItemMeta(autoMeta);
        inventory.setItem(50, autoLink);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(53, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 53) {
            new NodeEditorGUI(plugin, player, interaction, node).open();
            return;
        }

        if (slot < 45 && event.getCurrentItem() != null) {
            // Delete Option
            if (event.isRightClick()) {
                if (slot < node.getOptions().size()) {
                    node.getOptions().remove(slot);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    initialize();
                }
            }
        }

        if (slot == 49) {
            // Add Option
            player.closeInventory();
            player.sendMessage(Component.text("Enter option text (or 'cancel'):", NamedTextColor.GREEN));
            id.naturalsmp.naturalinteraction.util.ChatInput.capture(plugin, player, (text) -> {
                if (text.equalsIgnoreCase("cancel")) {
                    new OptionEditorGUI(plugin, player, interaction, node).open();
                    return;
                }
                
                // Prompt for Target: Existing or New
                // Since this is a simple chat flow, we can ask for behavior or use a GUI.
                // Re-open a temporary GUI to choose behavior
                new TargetSelectionGUI(plugin, player, interaction, text, node).open();
            });
        }
        
        if (slot == 50) {
            // Set Auto-Next
             new NodeSelectorGUI(plugin, player, interaction, this, (selectedNode) -> {
                 node.setNextNodeId(selectedNode == null ? null : selectedNode.getId());
                 plugin.getInteractionManager().saveInteraction(interaction);
                 new OptionEditorGUI(plugin, player, interaction, node).open();
            }).open();
        }
    }
    
    // Inner class (or separate) for Target Selection
    // For simplicity, defining here since it's tightly coupled to this flow
    private static class TargetSelectionGUI extends GUI {
        private final Interaction interaction;
        private final String optionText;
        private final DialogueNode parentNode;
        
        public TargetSelectionGUI(NaturalInteraction plugin, Player player, Interaction interaction, String optionText, DialogueNode parentNode) {
            super(plugin, player, 27, "Target for: " + optionText);
            this.interaction = interaction;
            this.optionText = optionText;
            this.parentNode = parentNode;
        }

        @Override
        public void initialize() {
            inventory.clear();
            
            ItemStack existing = new ItemStack(Material.BOOK);
            ItemMeta exMeta = existing.getItemMeta();
            exMeta.displayName(Component.text("Select Existing Node", NamedTextColor.YELLOW));
            existing.setItemMeta(exMeta);
            inventory.setItem(11, existing);
            
            ItemStack create = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta crMeta = create.getItemMeta();
            crMeta.displayName(Component.text("Create New Nested Node", NamedTextColor.GREEN));
            create.setItemMeta(crMeta);
            inventory.setItem(15, create);
        }

        @Override
        public void handleClick(InventoryClickEvent event) {
            int slot = event.getRawSlot();
            if (slot == 11) {
                // Select Existing
                new NodeSelectorGUI(plugin, player, interaction, this, (selectedNode) -> {
                     if (selectedNode != null) {
                         Option option = new Option(optionText, selectedNode.getId());
                         parentNode.getOptions().add(option);
                         plugin.getInteractionManager().saveInteraction(interaction);
                     }
                     new OptionEditorGUI(plugin, player, interaction, parentNode).open();
                }).open();
            } else if (slot == 15) {
                // Create New
                String newNodeId = "node_" + System.currentTimeMillis(); // Simple unique ID
                // Or "node_" + (interaction.getNodes().size() + 1) but risk of collision if deleted
                
                // Create Node
                DialogueNode newNode = new DialogueNode(newNodeId, "Replacing text...");
                interaction.addNode(newNode);
                
                // Link
                Option option = new Option(optionText, newNodeId);
                parentNode.getOptions().add(option);
                plugin.getInteractionManager().saveInteraction(interaction);
                
                // Open Editor for the new node immediately? Or back to option list?
                // User asked to "create dialogue inside option", so opening the new node seems appropriate
                player.sendMessage(Component.text("Created new node: " + newNodeId, NamedTextColor.GREEN));
                new NodeEditorGUI(plugin, player, interaction, newNode).open();
            }
        }
    }
}