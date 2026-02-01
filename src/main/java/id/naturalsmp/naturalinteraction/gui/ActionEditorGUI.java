package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.ActionType;
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

public class ActionEditorGUI extends GUI {
    private final Interaction interaction;
    private final DialogueNode node;

    public ActionEditorGUI(NaturalInteraction plugin, Player player, Interaction interaction, DialogueNode node) {
        super(plugin, player, 54, "Actions: " + node.getId());
        this.interaction = interaction;
        this.node = node;
    }

    @Override
    public void initialize() {
        inventory.clear();

        // List Actions
        for (int i = 0; i < node.getActions().size(); i++) {
            Action action = node.getActions().get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(action.getType().name(), NamedTextColor.AQUA));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Value: " + action.getValue(), NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Right-Click to Delete", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Add Action Buttons (Bottom Row)
        int slot = 45;
        for (ActionType type : ActionType.values()) {
            if (slot > 53) break;
            ItemStack item = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Add " + type.name(), NamedTextColor.GREEN));
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }

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

        if (slot < 45) {
            // Delete Action
            if (event.getCurrentItem() != null && event.isRightClick()) {
                if (slot < node.getActions().size()) {
                    node.getActions().remove(slot);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    initialize();
                }
            }
            return;
        }

        if (slot >= 45) {
            // Add Action
            String typeName = event.getCurrentItem().getItemMeta().getDisplayName(); 
            // Parsing display name is unsafe usually, better map slot to Enum
            int typeIndex = slot - 45;
            if (typeIndex < ActionType.values().length) {
                ActionType type = ActionType.values()[typeIndex];
                
                // Prompt for value
                player.closeInventory();
                player.sendMessage(Component.text("Enter value for " + type.name() + " (or 'cancel'):", NamedTextColor.GREEN));
                if (type == ActionType.TELEPORT) player.sendMessage(Component.text("Format: x,y,z,world,yaw,pitch (Type 'here' for current loc)", NamedTextColor.GRAY));
                if (type == ActionType.EFFECT) player.sendMessage(Component.text("Format: TYPE,duration(s),amplifier", NamedTextColor.GRAY));
                
                id.naturalsmp.naturalinteraction.utils.ChatInput.capture(plugin, player, (input) -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        String value = input;
                        if (type == ActionType.TELEPORT && input.equalsIgnoreCase("here")) {
                             org.bukkit.Location loc = player.getLocation();
                             value = loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getWorld().getName() + "," + loc.getYaw() + "," + loc.getPitch();
                        }
                        
                        node.getActions().add(new Action(type, value));
                        plugin.getInteractionManager().saveInteraction(interaction);
                        player.sendMessage(Component.text("Action added!", NamedTextColor.GREEN));
                    }
                    new ActionEditorGUI(plugin, player, interaction, node).open();
                });
            }
        }
    }
}
