package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.utils.ChatInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CommandRewardGUI extends GUI {
    private final Interaction interaction;
    private int page = 0;

    public CommandRewardGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 54, "Command Rewards");
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

        // Add Command Button
        ItemStack addCmd = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta addMeta = addCmd.getItemMeta();
        addMeta.displayName(Component.text(" Add Command Reward", NamedTextColor.GREEN, TextDecoration.BOLD));
        addMeta.lore(List.of(
            Component.text("Click to add a new command.", NamedTextColor.GRAY),
            Component.text("Example: /eco give %player_name% 1000", NamedTextColor.DARK_GRAY)
        ));
        addCmd.setItemMeta(addMeta);
        inventory.setItem(4, addCmd);

        // List Commands
        List<String> commands = interaction.getCommandRewards();
        int start = page * 36;
        int end = Math.min(start + 36, commands.size());
        
        for (int i = start; i < end; i++) {
            String cmd = commands.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(" /" + cmd, NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Left-Click to Edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Right-Click to Delete", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
            inventory.setItem(9 + (i - start), item);
        }

        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 49) {
            new RewardTypeGUI(plugin, player, interaction).open();
            return;
        }

        if (slot == 4) {
            player.closeInventory();
            player.sendMessage(Component.text("Enter command reward in chat (without / if console command, but usually commands are run as console):", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Placeholders: %player_name%", NamedTextColor.GRAY));
            ChatInput.capture(plugin, player, (input) -> {
                if (!input.equalsIgnoreCase("cancel")) {
                    interaction.getCommandRewards().add(input);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    player.sendMessage(Component.text("Command added!", NamedTextColor.GREEN));
                }
                new CommandRewardGUI(plugin, player, interaction).open();
            });
            return;
        }

        if (slot >= 9 && slot < 45) {
            int index = (page * 36) + (slot - 9);
            List<String> commands = interaction.getCommandRewards();
            
            if (index < commands.size()) {
                if (event.isRightClick()) {
                    commands.remove(index);
                    plugin.getInteractionManager().saveInteraction(interaction);
                    initialize();
                } else {
                    player.closeInventory();
                    String oldCmd = commands.get(index);
                    player.sendMessage(Component.text("Editing command: " + oldCmd, NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("Enter new command:", NamedTextColor.GREEN));
                    ChatInput.capture(plugin, player, (input) -> {
                        if (!input.equalsIgnoreCase("cancel")) {
                            interaction.getCommandRewards().set(index, input);
                            plugin.getInteractionManager().saveInteraction(interaction);
                            player.sendMessage(Component.text("Command updated!", NamedTextColor.GREEN));
                        }
                        new CommandRewardGUI(plugin, player, interaction).open();
                    });
                }
            }
        }
    }
}
