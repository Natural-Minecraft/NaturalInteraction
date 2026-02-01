package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RewardTypeGUI extends GUI {
    private final Interaction interaction;

    public RewardTypeGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 27, "Select Reward Type");
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
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 17) inventory.setItem(i, pane);
        }

        // Item Rewards
        ItemStack itemReward = new ItemStack(Material.CHEST);
        ItemMeta itemMeta = itemReward.getItemMeta();
        itemMeta.displayName(Component.text("Item Rewards", NamedTextColor.YELLOW, TextDecoration.BOLD));
        itemMeta.lore(List.of(
            Component.text("Give items to the player.", NamedTextColor.GRAY),
            Component.text("Click to edit.", NamedTextColor.YELLOW)
        ));
        itemReward.setItemMeta(itemMeta);
        inventory.setItem(11, itemReward);

        // Command Rewards
        ItemStack cmdReward = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta cmdMeta = cmdReward.getItemMeta();
        cmdMeta.displayName(Component.text("Command Rewards", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        cmdMeta.lore(List.of(
            Component.text("Run commands for the player.", NamedTextColor.GRAY),
            Component.text("Click to edit.", NamedTextColor.YELLOW)
        ));
        cmdReward.setItemMeta(cmdMeta);
        inventory.setItem(15, cmdReward);

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
            new SettingsGUI(plugin, player, interaction).open();
            return;
        }

        if (slot == 11) {
            new ItemRewardGUI(plugin, player, interaction).open();
        }

        if (slot == 15) {
            new CommandRewardGUI(plugin, player, interaction).open();
        }
    }
}