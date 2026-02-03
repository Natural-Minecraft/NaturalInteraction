package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.StoryWand;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AdminEditorGUI extends GUI {

    public AdminEditorGUI(NaturalInteraction plugin, Player player) {
        super(plugin, player, 27, "<gradient:#4facfe:#00f2fe>NaturalInteraction Editor</gradient>");
    }

    @Override
    public void initialize() {
        inventory.clear();

        // 11: List Interactions (Story Editor legacy term used in prompt)
        ItemStack list = new ItemStack(Material.BOOK);
        ItemMeta listMeta = list.getItemMeta();
        listMeta.displayName(net.kyori.adventure.text.Component.text("List Interactions", NamedTextColor.YELLOW));
        listMeta.lore(List
                .of(net.kyori.adventure.text.Component.text("Click to view and edit dialogues.", NamedTextColor.GRAY)));
        list.setItemMeta(listMeta);
        inventory.setItem(11, list);

        // 13: Create New Interaction
        ItemStack create = new ItemStack(Material.ANVIL);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.displayName(net.kyori.adventure.text.Component.text("Create New Interaction", NamedTextColor.GREEN));
        createMeta.lore(List.of(
                net.kyori.adventure.text.Component.text("Click to start a new dialogue set.", NamedTextColor.GRAY)));
        create.setItemMeta(createMeta);
        inventory.setItem(13, create);

        // 15: Get Wand Tool
        ItemStack wand = StoryWand.getWand();
        inventory.setItem(15, wand);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillMeta = filler.getItemMeta();
        fillMeta.displayName(net.kyori.adventure.text.Component.empty());
        filler.setItemMeta(fillMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 11) {
            // List Interactions (Using a new GUI or just showing a message for now)
            // For now, let's just open the existing Interaction List logic if it exists
            // Since we only have /ni edit <name>, we might need an InteractionListGUI
            player.sendMessage(net.kyori.adventure.text.Component.text("Use /ni edit <name> for now, list coming soon!",
                    NamedTextColor.YELLOW));
            player.closeInventory();
        } else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(net.kyori.adventure.text.Component.text("Enter name for new interaction (or 'cancel'):",
                    NamedTextColor.GREEN));
            id.naturalsmp.naturalinteraction.utils.ChatInput.capture(plugin, player, (name) -> {
                if (name.equalsIgnoreCase("cancel")) {
                    new AdminEditorGUI(plugin, player).open();
                    return;
                }
                if (plugin.getInteractionManager().hasInteraction(name)) {
                    player.sendMessage(net.kyori.adventure.text.Component
                            .text("Interaction '" + name + "' already exists.", NamedTextColor.RED));
                    new AdminEditorGUI(plugin, player).open();
                    return;
                }
                plugin.getInteractionManager().createInteraction(name);
                player.sendMessage(net.kyori.adventure.text.Component.text("Created interaction '" + name + "'.",
                        NamedTextColor.GREEN));
                new InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(name)).open();
            });
        } else if (slot == 15) {
            player.getInventory().addItem(StoryWand.getWand());
            player.sendMessage(
                    net.kyori.adventure.text.Component.text("Received Story Creator Wand!", NamedTextColor.GREEN));
            player.closeInventory();
        }
    }
}
