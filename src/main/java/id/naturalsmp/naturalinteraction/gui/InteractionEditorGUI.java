package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InteractionEditorGUI extends GUI {
    private final Interaction interaction;
    private int page = 0;

    public InteractionEditorGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 54, "Editing: " + interaction.getId());
        this.interaction = interaction;
    }

    @Override
    public void initialize() {
        inventory.clear();
        
        // Border
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.displayName(Component.empty());
        pane.setItemMeta(paneMeta);
        
        for (int i = 0; i < 9; i++) inventory.setItem(i, pane);
        for (int i = 45; i < 54; i++) inventory.setItem(i, pane);

        // Add Node Button
        ItemStack addNode = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta addMeta = addNode.getItemMeta();
        addMeta.displayName(Component.text(" Create New Node", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD));
        List<Component> addLore = new ArrayList<>();
        addLore.add(Component.text("Click to add a new dialogue node", NamedTextColor.GRAY));
        addLore.add(Component.text("to this interaction.", NamedTextColor.GRAY));
        addMeta.lore(addLore);
        addNode.setItemMeta(addMeta);
        inventory.setItem(4, addNode);

        // Nodes List
        List<DialogueNode> nodes = new ArrayList<>(interaction.getNodes().values());
        int start = page * 36;
        int end = Math.min(start + 36, nodes.size());
        
        for (int i = start; i < end; i++) {
            DialogueNode node = nodes.get(i);
            boolean isRoot = node.getId().equals(interaction.getRootNodeId());
            
            ItemStack item = new ItemStack(isRoot ? Material.BEACON : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(" Node: " + node.getId(), isRoot ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
            
            List<Component> lore = new ArrayList<>();
            if (isRoot) {
                lore.add(Component.text(" [STARTING POINT]", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD));
                lore.add(Component.text(""));
            }
            lore.add(Component.text("Text Preview:", NamedTextColor.GRAY));
            String preview = node.getText().length() > 30 ? node.getText().substring(0, 30) + "..." : node.getText();
            lore.add(Component.text(" \"" + preview + "\"", NamedTextColor.WHITE, net.kyori.adventure.text.format.TextDecoration.ITALIC));
            lore.add(Component.text(""));
            lore.add(Component.text("Options: " + node.getOptions().size(), NamedTextColor.GRAY));
            lore.add(Component.text("Actions: " + node.getActions().size(), NamedTextColor.GRAY));
            lore.add(Component.text("Duration: " + node.getDurationSeconds() + "s", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Left-Click to Edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Right-Click to Delete", NamedTextColor.RED));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(9 + (i - start), item);
        }

        // Settings Button
        ItemStack settings = new ItemStack(Material.REPEATER);
        ItemMeta setMeta = settings.getItemMeta();
        setMeta.displayName(Component.text(" Settings", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
        setMeta.lore(List.of(
            Component.text("Configure global settings", NamedTextColor.GRAY),
            Component.text("like rewards and cooldowns.", NamedTextColor.GRAY)
        ));
        settings.setItemMeta(setMeta);
        inventory.setItem(53, settings);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        if (slot == 4) {
            // Add Node
            String nodeId = "node_" + (interaction.getNodes().size() + 1);
            DialogueNode node = new DialogueNode(nodeId, "Hello World");
            interaction.addNode(node);
            
            // If first node, make it root
            if (interaction.getRootNodeId() == null) {
                interaction.setRootNodeId(nodeId);
            }
            
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
            return;
        }
        
        if (slot == 53) {
            new SettingsGUI(plugin, player, interaction).open();
            return;
        }

        if (slot >= 9 && slot < 45) {
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            
            // Parse Node ID from name is risky, better to map slot to index
            int index = (page * 36) + (slot - 9);
            List<DialogueNode> nodes = new ArrayList<>(interaction.getNodes().values());
            if (index < nodes.size()) {
                DialogueNode node = nodes.get(index);
                if (event.isRightClick()) {
                    interaction.getNodes().remove(node.getId());
                    if (node.getId().equals(interaction.getRootNodeId())) {
                        interaction.setRootNodeId(null);
                    }
                    plugin.getInteractionManager().saveInteraction(interaction);
                    initialize();
                } else {
                    // Open Node Editor
                    player.closeInventory(); // Close current
                    // new NodeEditorGUI(...)
                    player.sendMessage(Component.text("Opening node editor...", NamedTextColor.GREEN));
                    // TODO: Open Node Editor
                    new NodeEditorGUI(plugin, player, interaction, node).open();
                }
            }
            return;
        }
    }
}
