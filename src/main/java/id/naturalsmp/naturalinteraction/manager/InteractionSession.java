package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.Option;
import id.naturalsmp.naturalinteraction.session.CinematicController;
import id.naturalsmp.naturalinteraction.session.DialogueRenderer;
import id.naturalsmp.naturalinteraction.session.InventorySnapshot;
import id.naturalsmp.naturalinteraction.session.RewardDispatcher;
import id.naturalsmp.naturalinteraction.session.TimerController;
import id.naturalsmp.naturalinteraction.utils.ActionExecutor;
import id.naturalsmp.naturalinteraction.utils.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Coordinates a single player's dialogue session.
 *
 * All heavy logic is delegated to focused sub-controllers:
 *  - {@link InventorySnapshot}    inventory save / restore
 *  - {@link CinematicController}  NPC facing + movement lock
 *  - {@link DialogueRenderer}     typewriter, hologram, hotbar options
 *  - {@link TimerController}      BossBar countdown
 *  - {@link RewardDispatcher}     reward distribution
 */
public class InteractionSession {

    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;

    private final InventorySnapshot inventorySnapshot;
    private final CinematicController cinematicController;
    private final DialogueRenderer dialogueRenderer;
    private final TimerController timerController;

    private DialogueNode currentNode;

    public InteractionSession(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;

        this.inventorySnapshot   = new InventorySnapshot(player);
        this.cinematicController = new CinematicController(player, interaction);
        this.dialogueRenderer    = new DialogueRenderer(plugin, player, interaction);
        this.timerController     = new TimerController(plugin, player, interaction);
    }

    public Interaction getInteraction() { return interaction; }

    // ─── Session Start ────────────────────────────────────────────────────────

    public void start() {
        if (interaction.getRootNodeId() == null) {
            player.sendMessage(Component.text("Interaction has no starting point!", NamedTextColor.RED));
            return;
        }
        cinematicController.applyLock();
        playNode(interaction.getNode(interaction.getRootNodeId()));
    }

    public void startFromNode(String nodeId) {
        DialogueNode node = interaction.getNode(nodeId);
        if (node == null) {
            player.sendMessage(Component.text("Alternate node not found, using default.", NamedTextColor.YELLOW));
            start();
            return;
        }
        cinematicController.applyLock();
        playNode(node);
    }

    // ─── Node Playback ────────────────────────────────────────────────────────

    public void playNode(DialogueNode node) {
        if (node == null) {
            if (!interaction.isMandatory()) end();
            return;
        }

        dialogueRenderer.cancelAll();
        dialogueRenderer.clearHologram();
        timerController.stop();

        this.currentNode = node;

        // Execute node actions — may return a jump target node ID
        String jumpNodeId = executeActions(node);
        if (jumpNodeId != null) {
            DialogueNode target = interaction.getNode(jumpNodeId);
            if (target != null) { playNode(target); return; }
        }

        // Per-node rewards
        RewardDispatcher.dispatchNodeRewards(player, node, interaction,
                plugin.getInteractionManager().getCompletionTracker());

        // Face NPC
        cinematicController.faceNPC();

        // Optionally delay the dialogue start (for screen effects)
        int delayTicks = node.getDelayBeforeDialogueTicks();
        if (delayTicks > 0) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (!player.isOnline()) return;
                    startDialogue(node);
                }
            }.runTaskLater(plugin, delayTicks);
        } else {
            startDialogue(node);
        }
    }

    private void startDialogue(DialogueNode node) {
        dialogueRenderer.initHologram(cinematicController.findNPCLocation());
        dialogueRenderer.startTypewriter(node.getText(), node.getOptions());
        timerController.start(node, this::handleTimeout);
    }

    // ─── Timeout / Skip ───────────────────────────────────────────────────────

    private void handleTimeout() {
        if (currentNode.getOptions().isEmpty() && currentNode.getNextNodeId() != null) {
            playNode(interaction.getNode(currentNode.getNextNodeId()));
        } else if (currentNode.getOptions().isEmpty()) {
            end();
        } else if (interaction.isMandatory()) {
            timerController.start(currentNode, this::handleTimeout); // Reset timer — must choose
        } else {
            player.sendMessage(Component.text("⏱ Waktu habis.", NamedTextColor.RED));
            end();
        }
    }

    public void skip() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("Interaksi ini telah kedaluwarsa.", NamedTextColor.RED));
            return;
        }

        // First skip = complete typewriter instantly
        if (!dialogueRenderer.isTypewriterDone()) {
            dialogueRenderer.completeInstantly();
            return;
        }

        // Mandatory with options = player MUST pick one
        if (interaction.isMandatory() && currentNode != null && !currentNode.getOptions().isEmpty()) return;

        // Second skip = advance
        timerController.stop();
        timerController.setProgress(0);
        handleTimeout();
    }

    // ─── Option Selection (Cycle/Confirm model) ───────────────────────────────

    /** Cycle to the next option (Right-click / Scroll Down). */
    public void cycleNext() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) return;
        dialogueRenderer.cycleNext();
    }

    /** Cycle to the previous option (Scroll Up). */
    public void cyclePrev() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) return;
        dialogueRenderer.cyclePrev();
    }

    /** Jump directly to option index via hotbar slot. */
    public void jumpToSlot(int slot) {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) return;
        dialogueRenderer.jumpToSlot(slot);
    }

    /**
     * Confirm the currently highlighted option (Left-click / "F" key).
     * No-op if typewriter hasn't finished or no options are shown.
     */
    public void confirmSelected() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("Interaksi ini telah kedaluwarsa.", NamedTextColor.RED));
            return;
        }
        if (!dialogueRenderer.isDisplayingOptions()) return;
        Option chosen = dialogueRenderer.confirmSelected();
        if (chosen == null) return;

        timerController.stop();
        dialogueRenderer.cancelAll();
        playNode(interaction.getNode(chosen.getTargetNodeId()));
    }

    /** Check if player is currently viewing option choices. */
    public boolean isDisplayingOptions() {
        return dialogueRenderer.isDisplayingOptions();
    }

    // ─── Session End ──────────────────────────────────────────────────────────

    /**
     * Normal end: give rewards, restore inventory, clean up.
     */
    public void end() {
        timerController.stop();
        timerController.hide();
        dialogueRenderer.cancelAll();
        dialogueRenderer.clearHologram();
        dialogueRenderer.clearActionBar();
        cinematicController.removeLock();

        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        player.setInvisible(false);

        // Restore inventory (also clears leftover option papers)
        player.getInventory().clear();
        inventorySnapshot.restore(player);

        // If player was forced into prologue on join, restore pre-prologue save
        id.naturalsmp.naturalinteraction.listener.PrologueJoinListener prologueListener =
                plugin.getPrologueJoinListener();
        if (prologueListener != null && prologueListener.hasSavedData(player.getUniqueId())) {
            prologueListener.restorePlayerData(player, false);
        }

        plugin.getInteractionManager().endInteraction(player.getUniqueId());

        // Reset prologue NPC hologram (config-driven)
        resetPrologueNpcIfNeeded();

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

        // Rewards
        CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();
        boolean alreadyCompleted = tracker.hasCompleted(player.getUniqueId(), interaction.getId());
        RewardDispatcher.dispatch(player, interaction, inventorySnapshot, tracker, alreadyCompleted);

        if (!alreadyCompleted) {
            tracker.markCompleted(player.getUniqueId(), interaction.getId());
        }

        long cooldown = interaction.getCooldownSeconds();
        if (cooldown > 0) {
            plugin.getInteractionManager().setOnCooldown(player.getUniqueId(), interaction.getId(), cooldown);
        }
    }

    /**
     * Force-cleanup on disconnect — restore inventory WITHOUT giving rewards.
     * Prevents inventory corruption when players disconnect mid-interaction.
     */
    public void forceCleanup() {
        timerController.stop();
        timerController.hide();
        dialogueRenderer.cancelAll();
        dialogueRenderer.clearHologram();
        cinematicController.removeLock();

        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        player.setInvisible(false);

        if (player.isOnline()) {
            player.getInventory().clear();
            inventorySnapshot.restore(player);
            dialogueRenderer.clearActionBar();
        }

        plugin.getInteractionManager().endInteraction(player.getUniqueId());
        resetPrologueNpcIfNeeded();
    }

    // ─── Inventory Snapshot Accessors (used by ActionExecutor) ────────────────

    public InventorySnapshot getInventorySnapshot() { return inventorySnapshot; }

    /**
     * @deprecated Use {@link #getInventorySnapshot()} instead.
     */
    @Deprecated
    public org.bukkit.inventory.ItemStack[] getOriginalInventory() {
        return inventorySnapshot.getContents();
    }

    @Deprecated
    public boolean hasOriginalItem(String itemString, int amount) {
        return inventorySnapshot.hasItem(itemString, amount);
    }

    @Deprecated
    public void takeOriginalItem(String itemString, int amount) {
        inventorySnapshot.takeItem(itemString, amount);
    }

    @Deprecated
    public void addOriginalItem(org.bukkit.inventory.ItemStack itemToAdd) {
        org.bukkit.inventory.ItemStack overflow = inventorySnapshot.addItem(itemToAdd);
        if (overflow != null) player.getWorld().dropItemNaturally(player.getLocation(), overflow);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private String executeActions(DialogueNode node) {
        for (Action action : node.getActions()) {
            String jumpNodeId = ActionExecutor.execute(player, action, plugin);
            if (jumpNodeId != null) return jumpNodeId;
        }
        return null;
    }

    private void resetPrologueNpcIfNeeded() {
        String prologueId = PluginConfig.getPrologueInteractionId(plugin);
        if (!prologueId.equals(interaction.getId())) return;

        int npcId = PluginConfig.getPrologueNpcId(plugin);
        String hologramDefault = PluginConfig.getPrologueNpcHologramDefault(plugin);
        String hologramExclamation = PluginConfig.getPrologueNpcHologramExclamation(plugin);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select " + npcId);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc hologram set 0 " + hologramDefault);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc hologram add " + hologramExclamation);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc deselect");
    }
}
