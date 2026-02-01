package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryNode;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.GUIUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class AdminGUIEditor {

    private final NaturalInteraction plugin;

    public AdminGUIEditor(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = GUIUtils.createGUI(null, 27, "<gradient:#4facfe:#00f2fe>Story Editor</gradient>");

        inv.setItem(11,
                GUIUtils.createItem(Material.BOOK, "<yellow>List Stories", "<gray>Click to view all story nodes."));
        inv.setItem(13, GUIUtils.createItem(Material.ANVIL, "<green>Create New Story",
                "<gray>Click to start a new story node."));
        inv.setItem(15, GUIUtils.createItem(Material.BLAZE_ROD, "<aqua>Get Wand Tool",
                "<gray>Click to get the creation wand."));

        GUIUtils.fillEmpty(inv);
        player.openInventory(inv);
    }
}