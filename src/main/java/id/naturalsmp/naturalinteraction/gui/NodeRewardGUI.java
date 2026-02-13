package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for editing per-node command rewards.
 * Allows toggling reward on/off and managing reward commands.
 */
public class NodeRewardGUI extends GUI {
    private final Interaction interaction;
    private final DialogueNode node;

    public NodeRewardGUI(NaturalInteraction plugin, Player player, Interaction interaction, DialogueNode node) {
        super(plugin, player, 36, "Node Rewards: " + node.getId());
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
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, pane);
        for (int i = 27; i < 36; i++)
            inventory.setItem(i, pane);

        // Toggle Give Reward (slot 10)
        boolean active = node.isGiveReward();
        ItemStack toggle = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta toggleMeta = toggle.getItemMeta();
        toggleMeta.displayName(Component.text("Give Reward: " + (active ? "ON" : "OFF"),
                active ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> toggleLore = new ArrayList<>();
        toggleLore.add(Component.text("Jika ON, command rewards akan", NamedTextColor.GRAY));
        toggleLore.add(Component.text("dijalankan saat node ini dimainkan.", NamedTextColor.GRAY));
        toggleLore.add(Component.empty());
        toggleLore.add(Component.text("Klik untuk toggle", NamedTextColor.YELLOW));
        toggleMeta.lore(toggleLore);
        toggle.setItemMeta(toggleMeta);
        inventory.setItem(10, toggle);

        // Add Command (slot 12)
        ItemStack addCmd = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta addMeta = addCmd.getItemMeta();
        addMeta.displayName(Component.text("+ Add Command Reward", NamedTextColor.GREEN, TextDecoration.BOLD));
        List<Component> addLore = new ArrayList<>();
        addLore.add(Component.text("Tambah command yang dijalankan", NamedTextColor.GRAY));
        addLore.add(Component.text("dari console saat node ini dimainkan.", NamedTextColor.GRAY));
        addLore.add(Component.empty());
        addLore.add(Component.text("Placeholder: %player_name%", NamedTextColor.AQUA));
        addLore.add(Component.empty());
        addLore.add(Component.text("Contoh: give %player_name% diamond 5", NamedTextColor.WHITE));
        addMeta.lore(addLore);
        addCmd.setItemMeta(addMeta);
        inventory.setItem(12, addCmd);

        // List current command rewards (slots 14-25)
        List<String> commands = node.getCommandRewards();
        int startSlot = 14;
        for (int i = 0; i < commands.size() && startSlot + i < 27; i++) {
            String cmd = commands.get(i);
            ItemStack cmdItem = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta cmdMeta = cmdItem.getItemMeta();
            cmdMeta.displayName(Component.text("/" + cmd, NamedTextColor.GOLD));
            cmdMeta.lore(List.of(
                    Component.text("Right-Click to remove", NamedTextColor.RED)));
            cmdItem.setItemMeta(cmdMeta);
            inventory.setItem(startSlot + i, cmdItem);
        }

        // Info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("ℹ Info", NamedTextColor.AQUA));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Total Commands: " + commands.size(), NamedTextColor.WHITE));
        infoLore.add(Component.empty());
        infoLore.add(Component.text("Command dijalankan dari console", NamedTextColor.GRAY));
        infoLore.add(Component.text("(tanpa /) saat player memasuki", NamedTextColor.GRAY));
        infoLore.add(Component.text("node ini.", NamedTextColor.GRAY));
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        // Back (slot 31)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(31, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 31) {
            new NodeEditorGUI(plugin, player, interaction, node).open();
            return;
        }

        if (slot == 10) {
            // Toggle give reward
            node.setGiveReward(!node.isGiveReward());
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }

        if (slot == 12) {
            // Add new command reward via chat input
            player.closeInventory();
            player.sendMessage(
                    Component.text("Ketik command reward di chat (tanpa /), atau 'cancel':", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Placeholder: %player_name%", NamedTextColor.AQUA));
            id.naturalsmp.naturalinteraction.utils.ChatInput.capture(plugin, player, (input) -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    node.getCommandRewards().add(input);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    player.sendMessage(Component.text("Command reward ditambahkan: " + input, NamedTextColor.GREEN));
                }
                new NodeRewardGUI(plugin, player, interaction, node).open();
            });
        }

        // Remove command (right-click on command items)
        if (slot >= 14 && slot < 27 && event.isRightClick()) {
            int index = slot - 14;
            List<String> commands = node.getCommandRewards();
            if (index < commands.size()) {
                String removed = commands.remove(index);
                plugin.getInteractionManager().saveInteraction(interaction);
                player.sendMessage(Component.text("Removed: " + removed, NamedTextColor.RED));
                initialize();
            }
        }
    }
}
