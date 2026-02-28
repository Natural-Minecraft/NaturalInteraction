package id.naturalsmp.naturalinteraction.editor;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages the Hotbar Editor Mode for NaturalInteraction.
 * Players in editor mode get special hotbar items to create/edit interactions.
 * Items are dynamic: some only appear after selecting an interaction.
 */
public class EditorMode {

    private final NaturalInteraction plugin;

    // Track which players are in editor mode
    private final Map<UUID, EditorState> editorStates = new HashMap<>();

    public EditorMode(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    /**
     * Per-player editor state
     */
    public static class EditorState {
        private ItemStack[] savedInventory;
        private String selectedInteractionId;
        private boolean awaitingChatInput = false;
        private ChatInputType chatInputType = null;

        public enum ChatInputType {
            CREATE_INTERACTION,
            SET_UNICODE
        }

        public ItemStack[] getSavedInventory() {
            return savedInventory;
        }

        public void setSavedInventory(ItemStack[] inv) {
            this.savedInventory = inv;
        }

        public String getSelectedInteractionId() {
            return selectedInteractionId;
        }

        public void setSelectedInteractionId(String id) {
            this.selectedInteractionId = id;
        }

        public boolean isAwaitingChatInput() {
            return awaitingChatInput;
        }

        public void setAwaitingChatInput(boolean v) {
            this.awaitingChatInput = v;
        }

        public ChatInputType getChatInputType() {
            return chatInputType;
        }

        public void setChatInputType(ChatInputType type) {
            this.chatInputType = type;
        }
    }

    // ─── Toggle ────────────────────────────────────────────

    public void enterEditorMode(Player player) {
        if (isInEditorMode(player)) {
            player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                    .append(Component.text("Kamu sudah dalam Editor Mode!", NamedTextColor.RED)));
            return;
        }

        EditorState state = new EditorState();
        // Save current inventory
        state.setSavedInventory(player.getInventory().getContents().clone());
        editorStates.put(player.getUniqueId(), state);

        // Clear and set hotbar
        player.getInventory().clear();
        applyHotbar(player, state);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ╔══════════════════════════════════╗", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  ║ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("✦ Editor Mode Aktif", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("               ║", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("  ║ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Gunakan item di hotbar", NamedTextColor.GRAY))
                .append(Component.text("            ║", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("  ╚══════════════════════════════════╝", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    public void exitEditorMode(Player player) {
        EditorState state = editorStates.remove(player.getUniqueId());
        if (state == null)
            return;

        // Restore inventory
        player.getInventory().clear();
        if (state.getSavedInventory() != null) {
            player.getInventory().setContents(state.getSavedInventory());
        }

        player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                .append(Component.text("Editor Mode dimatikan. Inventory dikembalikan.", NamedTextColor.GREEN)));
    }

    public boolean isInEditorMode(Player player) {
        return editorStates.containsKey(player.getUniqueId());
    }

    public EditorState getState(Player player) {
        return editorStates.get(player.getUniqueId());
    }

    // ─── Hotbar Layout ────────────────────────────────────

    /**
     * Apply hotbar items based on current state.
     * If no interaction selected: only show Create, Select, Exit.
     * If interaction selected: show all tools.
     */
    public void applyHotbar(Player player, EditorState state) {
        player.getInventory().clear();

        // Slot 0: Create Interaction (always visible)
        player.getInventory().setItem(0, createItem(
                Material.EMERALD,
                "&a&lCreate Interaction",
                Arrays.asList("&7Buat interaction baru", "&7Nama akan diketik di chat"),
                true));

        // Slot 2: Select Interaction (always visible)
        player.getInventory().setItem(2, createItem(
                Material.BOOK,
                "&e&lSelect Interaction",
                Collections.singletonList("&7Pilih interaction untuk di-edit"),
                true));

        // Dynamic items — only if an interaction is selected
        if (state.getSelectedInteractionId() != null) {
            String selectedName = state.getSelectedInteractionId();

            // Slot 1: Bind to NPC
            player.getInventory().setItem(1, createItem(
                    Material.LEAD,
                    "&6&lBind to NPC",
                    Arrays.asList("&7Klik NPC untuk bind", "&7Interaction: &f" + selectedName),
                    true));

            // Slot 3: Test Play
            player.getInventory().setItem(3, createItem(
                    Material.ENDER_EYE,
                    "&b&lTest Play",
                    Arrays.asList("&7Preview interaction ini", "&7Interaction: &f" + selectedName),
                    true));

            // Slot 4: Node Editor
            player.getInventory().setItem(4, createItem(
                    Material.COMPASS,
                    "&d&lNode Editor",
                    Arrays.asList("&7Edit node dialog", "&7Interaction: &f" + selectedName),
                    true));

            // Slot 5: Set Unicode BG
            player.getInventory().setItem(5, createItem(
                    Material.PAINTING,
                    "&5&lSet Unicode BG",
                    Arrays.asList("&7Set karakter ItemsAdder", "&7untuk background dialog",
                            "&7Interaction: &f" + selectedName),
                    false));

            // Slot 6: Reload
            player.getInventory().setItem(6, createItem(
                    Material.CLOCK,
                    "&3&lReload",
                    Collections.singletonList("&7Reload semua interactions"),
                    false));
        } else {
            // Slot 6: Reload (always available even without selection)
            player.getInventory().setItem(6, createItem(
                    Material.CLOCK,
                    "&3&lReload",
                    Collections.singletonList("&7Reload semua interactions"),
                    false));
        }

        // Slot 8: Exit Editor (always visible)
        player.getInventory().setItem(8, createItem(
                Material.BARRIER,
                "&c&lExit Editor",
                Collections.singletonList("&7Keluar dari Editor Mode"),
                false));
    }

    /**
     * Called when an interaction is selected — refresh the hotbar with new tools
     */
    public void selectInteraction(Player player, String interactionId) {
        EditorState state = getState(player);
        if (state == null)
            return;

        state.setSelectedInteractionId(interactionId);
        applyHotbar(player, state);

        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("Interaction dipilih: ", NamedTextColor.YELLOW))
                .append(Component.text(interactionId, NamedTextColor.WHITE, TextDecoration.BOLD)));

        // Show in ActionBar too
        player.sendActionBar(Component.text("Editing: ", NamedTextColor.GRAY)
                .append(Component.text(interactionId, NamedTextColor.GOLD, TextDecoration.BOLD)));
    }

    // ─── Item Factory ─────────────────────────────────────

    private ItemStack createItem(Material material, String name, List<String> lore, boolean enchanted) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ChatUtils.toComponent(name));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(ChatUtils.toComponent(line));
        }
        meta.lore(loreComponents);

        if (enchanted) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // PDC tag to identify editor items
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "editor_item"),
                org.bukkit.persistence.PersistentDataType.BOOLEAN,
                true);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Check if an ItemStack is an editor hotbar item
     */
    public boolean isEditorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "editor_item"),
                org.bukkit.persistence.PersistentDataType.BOOLEAN);
    }

    // ─── Cleanup ──────────────────────────────────────────

    /**
     * Force-exit all players from editor mode (on plugin disable)
     */
    public void disableAll() {
        for (UUID uuid : new HashSet<>(editorStates.keySet())) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                exitEditorMode(player);
            }
        }
    }
}
