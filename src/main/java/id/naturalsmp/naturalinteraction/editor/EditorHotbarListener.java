package id.naturalsmp.naturalinteraction.editor;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.editor.EditorMode.EditorState;
import id.naturalsmp.naturalinteraction.gui.InteractionEditorGUI;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all hotbar editor interactions: item clicks, chat input, NPC binding,
 * etc.
 */
public class EditorHotbarListener implements Listener {

    private final NaturalInteraction plugin;

    public EditorHotbarListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    private EditorMode editor() {
        return plugin.getEditorMode();
    }

    // ─── Hotbar Item Click ────────────────────────────────

    @EventHandler
    public void onHotbarClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!editor().isInEditorMode(player))
            return;

        ItemStack item = event.getItem();
        if (item == null || !editor().isEditorItem(item))
            return;

        event.setCancelled(true);

        // Only trigger on right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        EditorState state = editor().getState(player);
        Material type = item.getType();

        switch (type) {
            case EMERALD -> handleCreateInteraction(player, state);
            case BOOK -> handleSelectInteraction(player);
            case LEAD -> handleBindToNPC(player, state);
            case ENDER_EYE -> handleTestPlay(player, state);
            case COMPASS -> handleNodeEditor(player, state);
            case PAINTING -> handleSetUnicode(player, state);
            case CLOCK -> handleReload(player);
            case BARRIER -> handleExitEditor(player);
        }
    }

    // ─── Slot 0: Create Interaction ───────────────────────

    private void handleCreateInteraction(Player player, EditorState state) {
        state.setAwaitingChatInput(true);
        state.setChatInputType(EditorState.ChatInputType.CREATE_INTERACTION);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ✦ ", NamedTextColor.GOLD)
                .append(Component.text("Ketik nama interaction baru di chat:", NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("    Contoh: ", NamedTextColor.GRAY)
                .append(Component.text("fishing_quest", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("    Ketik ", NamedTextColor.GRAY)
                .append(Component.text("cancel", NamedTextColor.RED))
                .append(Component.text(" untuk membatalkan.", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }

    // ─── Slot 2: Select Interaction ───────────────────────

    private void handleSelectInteraction(Player player) {
        java.util.Set<String> ids = plugin.getInteractionManager().getInteractionIds();
        if (ids.isEmpty()) {
            player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                    .append(Component.text("Belum ada interaction. Buat dulu!", NamedTextColor.RED)));
            return;
        }

        // Open a simple chest GUI with interaction list
        new EditorSelectGUI(plugin, player).open();
    }

    // ─── Slot 1: Bind to NPC ──────────────────────────────

    private void handleBindToNPC(Player player, EditorState state) {
        if (state.getSelectedInteractionId() == null)
            return;

        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("Klik kanan NPC yang ingin di-bind dengan: ", NamedTextColor.YELLOW))
                .append(Component.text(state.getSelectedInteractionId(), NamedTextColor.WHITE, TextDecoration.BOLD)));
        player.sendMessage(Component.text("  Item Lead harus ada di tangan.", NamedTextColor.GRAY));
    }

    // ─── Slot 3: Test Play ────────────────────────────────

    private void handleTestPlay(Player player, EditorState state) {
        if (state.getSelectedInteractionId() == null)
            return;

        // Temporarily exit editor mode, start interaction, then re-enter
        String interactionId = state.getSelectedInteractionId();
        editor().exitEditorMode(player);
        plugin.getInteractionManager().startInteraction(player, interactionId);
    }

    // ─── Slot 4: Node Editor ──────────────────────────────

    private void handleNodeEditor(Player player, EditorState state) {
        if (state.getSelectedInteractionId() == null)
            return;

        Interaction interaction = plugin.getInteractionManager().getInteraction(state.getSelectedInteractionId());
        if (interaction == null) {
            player.sendMessage(Component.text("Interaction tidak ditemukan!", NamedTextColor.RED));
            return;
        }
        new InteractionEditorGUI(plugin, player, interaction).open();
    }

    // ─── Slot 5: Set Unicode BG ───────────────────────────

    private void handleSetUnicode(Player player, EditorState state) {
        if (state.getSelectedInteractionId() == null)
            return;

        state.setAwaitingChatInput(true);
        state.setChatInputType(EditorState.ChatInputType.SET_UNICODE);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ✦ ", NamedTextColor.GOLD)
                .append(Component.text("Ketik karakter unicode ItemsAdder di chat:", NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("    Contoh: ", NamedTextColor.GRAY)
                .append(Component.text(":dialogue_bg:", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("    Ketik ", NamedTextColor.GRAY)
                .append(Component.text("cancel", NamedTextColor.RED))
                .append(Component.text(" untuk batal, ", NamedTextColor.GRAY))
                .append(Component.text("clear", NamedTextColor.YELLOW))
                .append(Component.text(" untuk hapus unicode.", NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }

    // ─── Slot 6: Reload ───────────────────────────────────

    private void handleReload(Player player) {
        plugin.reloadPlugin();
        player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text("Semua interactions di-reload!", NamedTextColor.GREEN)));
    }

    // ─── Slot 8: Exit ─────────────────────────────────────

    private void handleExitEditor(Player player) {
        editor().exitEditorMode(player);
    }

    // ─── Chat Input Handler ───────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!editor().isInEditorMode(player))
            return;

        EditorState state = editor().getState(player);
        if (state == null || !state.isAwaitingChatInput())
            return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        state.setAwaitingChatInput(false);
        EditorState.ChatInputType inputType = state.getChatInputType();
        state.setChatInputType(null);

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("✦ Dibatalkan.", NamedTextColor.YELLOW));
            return;
        }

        // Schedule on main thread (chat event is async)
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            switch (inputType) {
                case CREATE_INTERACTION -> {
                    String id = message.toLowerCase().replace(" ", "_");

                    if (plugin.getInteractionManager().hasInteraction(id)) {
                        player.sendMessage(Component.text("✦ ", NamedTextColor.RED)
                                .append(Component.text("Interaction '" + id + "' sudah ada!", NamedTextColor.RED)));
                        return;
                    }

                    plugin.getInteractionManager().createInteraction(id);
                    player.sendMessage(Component.text("✦ ", NamedTextColor.GREEN)
                            .append(Component.text("Interaction ", NamedTextColor.GREEN))
                            .append(Component.text(id, NamedTextColor.WHITE, TextDecoration.BOLD))
                            .append(Component.text(" berhasil dibuat!", NamedTextColor.GREEN)));

                    // Auto-select the newly created interaction
                    editor().selectInteraction(player, id);
                }
                case SET_UNICODE -> {
                    String selectedId = state.getSelectedInteractionId();
                    if (selectedId == null)
                        return;

                    Interaction interaction = plugin.getInteractionManager().getInteraction(selectedId);
                    if (interaction == null)
                        return;

                    if (message.equalsIgnoreCase("clear")) {
                        interaction.setDialogueUnicode("");
                        plugin.getInteractionManager().saveInteraction(interaction);
                        player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                                .append(Component.text("Unicode background dihapus.", NamedTextColor.YELLOW)));
                    } else {
                        interaction.setDialogueUnicode(message);
                        plugin.getInteractionManager().saveInteraction(interaction);
                        player.sendMessage(Component.text("✦ ", NamedTextColor.GREEN)
                                .append(Component.text("Unicode background diset ke: ", NamedTextColor.GREEN))
                                .append(Component.text(message, NamedTextColor.WHITE, TextDecoration.BOLD)));
                    }
                }
            }
        });
    }

    // ─── NPC Binding (while holding Lead in editor) ───────

    @EventHandler(priority = EventPriority.HIGH)
    public void onNPCClickEditor(NPCRightClickEvent event) {
        Player player = event.getClicker();
        if (!editor().isInEditorMode(player))
            return;

        EditorState state = editor().getState(player);
        if (state == null || state.getSelectedInteractionId() == null)
            return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.LEAD || !editor().isEditorItem(hand))
            return;

        event.setCancelled(true);
        NPC npc = event.getNPC();
        String interactionId = state.getSelectedInteractionId();

        // Add or update the InteractionTrait
        if (!npc.hasTrait(InteractionTrait.class)) {
            npc.addTrait(InteractionTrait.class);
        }
        InteractionTrait trait = npc.getTrait(InteractionTrait.class);
        trait.setInteractionId(interactionId);

        player.sendMessage(Component.text("✦ ", NamedTextColor.GREEN)
                .append(Component.text("NPC ", NamedTextColor.GREEN))
                .append(Component.text(npc.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" berhasil di-bind ke ", NamedTextColor.GREEN))
                .append(Component.text(interactionId, NamedTextColor.GOLD, TextDecoration.BOLD)));
    }

    // ─── Prevent dropping editor items ────────────────────

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (editor().isInEditorMode(event.getPlayer())) {
            if (editor().isEditorItem(event.getItemDrop().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    // ─── Prevent moving editor items in inventory ─────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!editor().isInEditorMode(player))
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && editor().isEditorItem(clicked)) {
            event.setCancelled(true);
        }
    }

    // ─── Cleanup on quit ──────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (editor().isInEditorMode(event.getPlayer())) {
            editor().exitEditorMode(event.getPlayer());
        }
    }
}
