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
import java.util.function.Consumer;

public class NodeSelectorGUI extends GUI {
    private final Interaction interaction;
    private final Consumer<DialogueNode> callback;
    private final GUI parent;
    private int page = 0;

    public NodeSelectorGUI(NaturalInteraction plugin, Player player, Interaction interaction, GUI parent, Consumer<DialogueNode> callback) {
        super(plugin, player, 54, "Select Node");
        this.interaction = interaction;
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    public void initialize() {
        inventory.clear();
        
        List<DialogueNode> nodes = new ArrayList<>(interaction.getNodes().values());
        int start = page * 45;
        int end = Math.min(start + 45, nodes.size());
        
        for (int i = start; i < end; i++) {
            DialogueNode node = nodes.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(node.getId(), NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(node.getText().length() > 30 ? node.getText().substring(0, 30) + "..." : node.getText(), NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }

        // Back / Cancel
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Cancel", NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inventory.setItem(53, close);
        
        // Remove Selection (None)
        ItemStack none = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta noneMeta = none.getItemMeta();
        noneMeta.displayName(Component.text("Unlink (None)", NamedTextColor.RED));
        none.setItemMeta(noneMeta);
        inventory.setItem(45, none);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        if (slot == 53) {
            parent.open();
            return;
        }
        
        if (slot == 45) {
            callback.accept(null); // Unlink
            return;
        }

        if (slot >= 0 && slot < 45) {
            int index = (page * 45) + slot;
            List<DialogueNode> nodes = new ArrayList<>(interaction.getNodes().values());
            if (index < nodes.size()) {
                callback.accept(nodes.get(index));
            }
        }
    }
}
