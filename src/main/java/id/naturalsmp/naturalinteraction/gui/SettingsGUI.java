package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.PostCompletionMode;
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

public class SettingsGUI extends GUI {
    private final Interaction interaction;

    public SettingsGUI(NaturalInteraction plugin, Player player, Interaction interaction) {
        super(plugin, player, 45, "Settings: " + interaction.getId());
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
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, pane);
        for (int i = 36; i < 45; i++)
            inventory.setItem(i, pane);

        // === Row 1: Core Settings ===

        // One Time Reward Toggle (slot 10)
        ItemStack oneTime = new ItemStack(interaction.isOneTimeReward() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta otMeta = oneTime.getItemMeta();
        otMeta.displayName(Component.text("One Time Reward: " + (interaction.isOneTimeReward() ? "ON" : "OFF"),
                interaction.isOneTimeReward() ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> otLore = new ArrayList<>();
        otLore.add(Component.text("Jika ON, reward hanya diberikan", NamedTextColor.GRAY));
        otLore.add(Component.text("1x per player.", NamedTextColor.GRAY));
        otLore.add(Component.empty());
        otLore.add(Component.text("Klik untuk toggle", NamedTextColor.YELLOW));
        otMeta.lore(otLore);
        oneTime.setItemMeta(otMeta);
        inventory.setItem(10, oneTime);

        // Cooldown (slot 12)
        ItemStack cooldown = new ItemStack(Material.CLOCK);
        ItemMeta cdMeta = cooldown.getItemMeta();
        cdMeta.displayName(Component.text("Cooldown: " + interaction.getCooldownSeconds() + "s", NamedTextColor.GOLD));
        cdMeta.lore(List.of(
                Component.text("Left-Click: +5s", NamedTextColor.GRAY),
                Component.text("Right-Click: -5s", NamedTextColor.GRAY)));
        cooldown.setItemMeta(cdMeta);
        inventory.setItem(12, cooldown);

        // Edit Rewards (slot 14)
        ItemStack rewards = new ItemStack(Material.EMERALD);
        ItemMeta rMeta = rewards.getItemMeta();
        rMeta.displayName(
                Component.text("Edit Rewards (" + interaction.getRewards().size() + ")", NamedTextColor.GOLD));
        rMeta.lore(List.of(Component.text("Click to manage rewards.", NamedTextColor.GRAY)));
        rewards.setItemMeta(rMeta);
        inventory.setItem(14, rewards);

        // === Row 2: Post-Completion Settings (only visible when oneTimeReward is ON)
        // ===

        if (interaction.isOneTimeReward()) {
            // Section Header
            ItemStack header = new ItemStack(Material.NETHER_STAR);
            ItemMeta headerMeta = header.getItemMeta();
            headerMeta.displayName(
                    Component.text("⚙ Post-Completion Settings", NamedTextColor.AQUA, TextDecoration.BOLD));
            headerMeta.lore(List.of(
                    Component.text("Pengaturan apa yang terjadi", NamedTextColor.GRAY),
                    Component.text("saat player interaksi ulang.", NamedTextColor.GRAY)));
            header.setItemMeta(headerMeta);
            inventory.setItem(16, header);

            // Post Completion Mode (slot 19)
            PostCompletionMode mode = interaction.getPostCompletionMode();
            boolean isAlternate = mode == PostCompletionMode.ALTERNATE_NODES;
            ItemStack modeItem = new ItemStack(isAlternate ? Material.COMPARATOR : Material.REPEATER);
            ItemMeta modeMeta = modeItem.getItemMeta();
            modeMeta.displayName(Component.text("Mode: " + mode.name(), NamedTextColor.AQUA));
            List<Component> modeLore = new ArrayList<>();
            modeLore.add(Component.empty());
            modeLore.add(Component.text("SAME_NODES:", NamedTextColor.GRAY));
            modeLore.add(Component.text("  Ulang dialog yang sama", NamedTextColor.WHITE));
            modeLore.add(Component.text("ALTERNATE_NODES:", NamedTextColor.GRAY));
            modeLore.add(Component.text("  Gunakan root node berbeda", NamedTextColor.WHITE));
            modeLore.add(Component.text("  untuk player yang sudah selesai", NamedTextColor.WHITE));
            modeLore.add(Component.empty());
            modeLore.add(Component.text("Klik untuk toggle", NamedTextColor.YELLOW));
            modeMeta.lore(modeLore);
            modeItem.setItemMeta(modeMeta);
            inventory.setItem(19, modeItem);

            // Alternate Root Node Selector (slot 21, only if ALTERNATE_NODES)
            if (isAlternate) {
                ItemStack altRoot = new ItemStack(Material.END_CRYSTAL);
                ItemMeta altMeta = altRoot.getItemMeta();
                String currentAlt = interaction.getPostCompletionRootNodeId();
                altMeta.displayName(Component.text("Alternate Root Node", NamedTextColor.LIGHT_PURPLE));
                List<Component> altLore = new ArrayList<>();
                altLore.add(Component.text("Current: " + (currentAlt != null ? currentAlt : "Not Set"),
                        currentAlt != null ? NamedTextColor.GREEN : NamedTextColor.RED));
                altLore.add(Component.empty());
                altLore.add(Component.text("Node yang digunakan sebagai", NamedTextColor.GRAY));
                altLore.add(Component.text("starting point untuk player", NamedTextColor.GRAY));
                altLore.add(Component.text("yang sudah pernah selesai.", NamedTextColor.GRAY));
                altLore.add(Component.empty());
                altLore.add(Component.text("Klik untuk pilih node", NamedTextColor.YELLOW));
                altMeta.lore(altLore);
                altRoot.setItemMeta(altMeta);
                inventory.setItem(21, altRoot);
            }
        }

        // Back (slot 40)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(backMeta);
        inventory.setItem(40, back);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 40) {
            new InteractionEditorGUI(plugin, player, interaction).open();
            return;
        }

        if (slot == 10) {
            interaction.setOneTimeReward(!interaction.isOneTimeReward());
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }

        if (slot == 12) {
            long change = event.isRightClick() ? -5 : 5;
            interaction.setCooldownSeconds(Math.max(0, interaction.getCooldownSeconds() + change));
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }

        if (slot == 14) {
            new RewardTypeGUI(plugin, player, interaction).open();
        }

        if (slot == 19 && interaction.isOneTimeReward()) {
            // Toggle Post Completion Mode
            PostCompletionMode current = interaction.getPostCompletionMode();
            PostCompletionMode next = (current == PostCompletionMode.SAME_NODES)
                    ? PostCompletionMode.ALTERNATE_NODES
                    : PostCompletionMode.SAME_NODES;
            interaction.setPostCompletionMode(next);
            plugin.getInteractionManager().saveInteraction(interaction);
            initialize();
        }

        if (slot == 21 && interaction.isOneTimeReward()
                && interaction.getPostCompletionMode() == PostCompletionMode.ALTERNATE_NODES) {
            // Open Node Selector for alternate root
            new NodeSelectorGUI(plugin, player, interaction, this, (selected) -> {
                if (selected == null) {
                    interaction.setPostCompletionRootNodeId(null);
                    player.sendMessage(Component.text("Alternate root node cleared.", NamedTextColor.YELLOW));
                } else {
                    interaction.setPostCompletionRootNodeId(selected.getId());
                    player.sendMessage(
                            Component.text("Alternate root set to: " + selected.getId(), NamedTextColor.GREEN));
                }
                plugin.getInteractionManager().saveInteraction(interaction);
                this.open();
            }).open();
        }
    }
}
