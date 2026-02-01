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

public class NodeEditorGUI extends GUI {
    private final Interaction interaction;
    private final DialogueNode node;

    public NodeEditorGUI(NaturalInteraction plugin, Player player, Interaction interaction, DialogueNode node) {
        super(plugin, player, 27, "Node: " + node.getId());
        this.interaction = interaction;
        this.node = node;
    }

    @Override
    public void initialize() {
        inventory.clear();
        
        // Border
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.displayName(Component.empty());
        pane.setItemMeta(paneMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17) inventory.setItem(i, pane);
        }

        // 0: Edit Text
        ItemStack textItem = new ItemStack(Material.BOOK);
        ItemMeta textMeta = textItem.getItemMeta();
        textMeta.displayName(Component.text("Edit Text", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current:", NamedTextColor.GRAY));
        lore.add(Component.text(node.getText(), NamedTextColor.WHITE));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to change", NamedTextColor.YELLOW));
        textMeta.lore(lore);
        textItem.setItemMeta(textMeta);
        inventory.setItem(10, textItem);

        // 1: Edit Actions
        ItemStack actionItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta actionMeta = actionItem.getItemMeta();
        actionMeta.displayName(Component.text("Edit Actions", NamedTextColor.AQUA));
        List<Component> actLore = new ArrayList<>();
        actLore.add(Component.text("Count: " + node.getActions().size(), NamedTextColor.GRAY));
        actLore.add(Component.text(""));
        actLore.add(Component.text("Manage commands, sounds,", NamedTextColor.GRAY));
        actLore.add(Component.text("effects, and more.", NamedTextColor.GRAY));
        actionMeta.lore(actLore);
        actionItem.setItemMeta(actionMeta);
        inventory.setItem(12, actionItem);

        // 2: Edit Options
        ItemStack optItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta optMeta = optItem.getItemMeta();
        optMeta.displayName(Component.text("Edit Options", NamedTextColor.GOLD));
        List<Component> optLore = new ArrayList<>();
        optLore.add(Component.text("Count: " + node.getOptions().size(), NamedTextColor.GRAY));
        optLore.add(Component.text(""));
        optLore.add(Component.text("Manage choices and branching.", NamedTextColor.GRAY));
        optMeta.lore(optLore);
        optItem.setItemMeta(optMeta);
        inventory.setItem(14, optItem);
        
        // 3: Set Duration
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta clockMeta = clock.getItemMeta();
        clockMeta.displayName(Component.text("Duration: " + node.getDurationSeconds() + "s", NamedTextColor.GREEN));
        clockMeta.lore(List.of(
            Component.text("Left-Click: +1s", NamedTextColor.GRAY),
            Component.text("Right-Click: -1s", NamedTextColor.GRAY)
        ));
        clock.setItemMeta(clockMeta);
        inventory.setItem(16, clock);
        
        // 4: Toggle Skip
        Material skipMat = node.isSkippable() ? Material.LIME_DYE : Material.GRAY_DYE;
        NamedTextColor skipColor = node.isSkippable() ? NamedTextColor.GREEN : NamedTextColor.RED;
        ItemStack skipItem = new ItemStack(skipMat);
        ItemMeta skipMeta = skipItem.getItemMeta();
        skipMeta.displayName(Component.text("Skip Button: " + (node.isSkippable() ? "ON" : "OFF"), skipColor));
        skipMeta.lore(List.of(Component.text("Click to toggle skip button functionality", NamedTextColor.GRAY)));
        skipItem.setItemMeta(skipMeta);
        inventory.setItem(17, skipItem); // Placed next to duration
        
        // 5: Link Next Dialogue
        ItemStack linkItem = new ItemStack(Material.CHAIN);
        ItemMeta linkMeta = linkItem.getItemMeta();
        linkMeta.displayName(Component.text("Link Next Node", NamedTextColor.BLUE));
        List<Component> linkLore = new ArrayList<>();
        linkLore.add(Component.text("Current: " + (node.getNextNodeId() == null ? "None" : node.getNextNodeId()), NamedTextColor.GRAY));
        linkLore.add(Component.text(""));
        linkLore.add(Component.text("Click to select next node", NamedTextColor.BLUE));
        linkLore.add(Component.text("to jump to without options.", NamedTextColor.BLUE));
        linkMeta.lore(linkLore);
        linkItem.setItemMeta(linkMeta);
        inventory.setItem(13, linkItem); // Center

        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(22, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        if (slot == 22) {
            new InteractionEditorGUI(plugin, player, interaction).open();
            return;
        }

        if (slot == 10) { // Text
            player.closeInventory();
            player.sendMessage(Component.text("Enter new dialogue text in chat (or 'cancel'):", NamedTextColor.GREEN));
            id.naturalsmp.naturalinteraction.util.ChatInput.capture(plugin, player, (input) -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    node.setText(input);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    player.sendMessage(Component.text("Text updated!", NamedTextColor.GREEN));
                }
                new NodeEditorGUI(plugin, player, interaction, node).open();
            });
        }
        
        if (slot == 16) { // Duration
            if (event.isLeftClick()) node.setDurationSeconds(node.getDurationSeconds() + 1);
            else if (event.isRightClick() && node.getDurationSeconds() > 0) node.setDurationSeconds(node.getDurationSeconds() - 1);
            plugin.getInteractionManager().saveInteraction(interaction); // Save on change
            initialize(); 
        }
        
        if (slot == 17) { // Skip Toggle
            node.setSkippable(!node.isSkippable());
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }

        if (slot == 13) { // Link Next Dialogue
            new NodeSelectorGUI(plugin, player, interaction, this, (selected) -> {
                if (selected == null) {
                     node.setNextNodeId(null);
                     player.sendMessage(Component.text("Unlinked next node.", NamedTextColor.YELLOW));
                } else {
                     if (selected.getId().equals(node.getId())) {
                         player.sendMessage(Component.text("Cannot link to itself.", NamedTextColor.RED));
                     } else {
                         node.setNextNodeId(selected.getId());
                         player.sendMessage(Component.text("Linked to " + selected.getId(), NamedTextColor.GREEN));
                     }
                }
                plugin.getInteractionManager().saveInteraction(interaction);
                this.open(); // Re-open NodeEditor
            }).open();
        }
        
        if (slot == 12) { // Actions
             new ActionEditorGUI(plugin, player, interaction, node).open();
        }
        
        if (slot == 14) { // Options
             new OptionEditorGUI(plugin, player, interaction, node).open();
        }
    }
}