package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
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

public class SettingsGUI extends GUI {
    private final Interaction interaction;

    public SettingsGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 27, "Settings: " + interaction.getId());
        this.interaction = interaction;
    }

    @Override
    public void initialize() {
        inventory.clear();

        // One Time Reward Toggle
        ItemStack oneTime = new ItemStack(interaction.isOneTimeReward() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta otMeta = oneTime.getItemMeta();
        otMeta.displayName(Component.text("One Time Reward: " + interaction.isOneTimeReward(), NamedTextColor.GOLD));
        oneTime.setItemMeta(otMeta);
        inventory.setItem(11, oneTime);

        // Cooldown
        ItemStack cooldown = new ItemStack(Material.CLOCK);
        ItemMeta cdMeta = cooldown.getItemMeta();
        cdMeta.displayName(Component.text("Cooldown: " + interaction.getCooldownSeconds() + "s", NamedTextColor.GOLD));
        cdMeta.lore(List.of(Component.text("Click to Increase, Right to Decrease", NamedTextColor.GRAY)));
        cooldown.setItemMeta(cdMeta);
        inventory.setItem(13, cooldown);

        // Edit Rewards
        ItemStack rewards = new ItemStack(Material.EMERALD);
        ItemMeta rMeta = rewards.getItemMeta();
        rMeta.displayName(Component.text("Edit Rewards (" + interaction.getRewards().size() + ")", NamedTextColor.GOLD));
        rMeta.lore(List.of(Component.text("Click to manage rewards.", NamedTextColor.GRAY)));
        rewards.setItemMeta(rMeta);
        inventory.setItem(15, rewards);

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

        if (slot == 11) {
            interaction.setOneTimeReward(!interaction.isOneTimeReward());
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }
        
        if (slot == 13) {
            long change = event.isRightClick() ? -5 : 5;
            interaction.setCooldownSeconds(Math.max(0, interaction.getCooldownSeconds() + change));
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }
        
        if (slot == 15) {
             new RewardTypeGUI(plugin, player, interaction).open();
        }
    }
}