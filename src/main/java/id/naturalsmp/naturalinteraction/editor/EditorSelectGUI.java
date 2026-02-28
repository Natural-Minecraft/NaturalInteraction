package id.naturalsmp.naturalinteraction.editor;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GUI for selecting an interaction to edit in the hotbar editor.
 */
public class EditorSelectGUI implements Listener {

    private final NaturalInteraction plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<String> interactionIds = new ArrayList<>();

    public EditorSelectGUI(NaturalInteraction plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        interactionIds.addAll(plugin.getInteractionManager().getInteractionIds());
        Collections.sort(interactionIds);

        int size = Math.min(54, ((interactionIds.size() / 9) + 1) * 9);
        if (size < 9)
            size = 9;

        this.inventory = Bukkit.createInventory(null, size,
                Component.text("✦ Select Interaction", NamedTextColor.GOLD, TextDecoration.BOLD));

        populate();
    }

    private void populate() {
        for (int i = 0; i < interactionIds.size() && i < inventory.getSize(); i++) {
            String id = interactionIds.get(i);

            // Check if this interaction is currently selected
            EditorMode.EditorState state = plugin.getEditorMode().getState(player);
            boolean isSelected = state != null && id.equals(state.getSelectedInteractionId());

            Material mat = isSelected ? Material.ENCHANTED_BOOK : Material.PAPER;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            String displayName = isSelected ? "&a&l✦ " + id + " (Dipilih)" : "&e" + id;
            meta.displayName(ChatUtils.toComponent(displayName));

            // Node count info
            var interaction = plugin.getInteractionManager().getInteraction(id);
            int nodeCount = interaction != null ? interaction.getNodes().size() : 0;
            String unicode = interaction != null ? interaction.getDialogueUnicode() : "";

            List<Component> lore = new ArrayList<>();
            lore.add(ChatUtils.toComponent("&7Nodes: &f" + nodeCount));
            if (!unicode.isEmpty()) {
                lore.add(ChatUtils.toComponent("&7Unicode: &f" + unicode));
            }
            lore.add(Component.empty());
            lore.add(ChatUtils.toComponent("&eKlik untuk memilih"));
            meta.lore(lore);

            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
    }

    public void open() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory)
            return;
        if (!(event.getWhoClicked() instanceof Player clicker))
            return;
        if (!clicker.getUniqueId().equals(player.getUniqueId()))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= interactionIds.size())
            return;

        String selectedId = interactionIds.get(slot);
        player.closeInventory();
        plugin.getEditorMode().selectInteraction(player, selectedId);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory() != inventory)
            return;
        HandlerList.unregisterAll(this);
    }
}
