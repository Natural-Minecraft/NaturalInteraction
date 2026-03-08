package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.Option;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class InteractionSession {
    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;
    private DialogueNode currentNode;
    private BukkitRunnable timerTask;
    private BukkitRunnable typewriterTask;
    private BukkitRunnable actionBarTask;
    private BossBar bossBar;
    private final java.util.List<org.bukkit.entity.Entity> choiceEntities = new java.util.ArrayList<>();
    private org.bukkit.entity.TextDisplay mainTextDisplay;
    private org.bukkit.inventory.ItemStack[] originalInventory;
    private boolean displayingOptions = false;

    // Typewriter state
    private String fullDialogueText = "";
    private int revealedWords = 0;
    private boolean typewriterDone = false;

    public InteractionSession(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;
    }

    public void start() {
        if (interaction.getRootNodeId() == null) {
            player.sendMessage(Component.text("Interaction has no starting point!", NamedTextColor.RED));
            return;
        }

        // Save inventory
        originalInventory = player.getInventory().getContents();

        // Apply cinematic focus lock
        applyCinematicLock();

        playNode(interaction.getNode(interaction.getRootNodeId()));
    }

    /**
     * Start from a specific node (used for post-completion alternate flow)
     */
    public void startFromNode(String nodeId) {
        DialogueNode node = interaction.getNode(nodeId);
        if (node == null) {
            player.sendMessage(Component.text("Alternate node not found, using default.", NamedTextColor.YELLOW));
            start();
            return;
        }
        applyCinematicLock();
        playNode(node);
    }

    /**
     * Apply cinematic movement lock and zoom effect
     */
    private void applyCinematicLock() {
        // Slowness 255 = complete freeze (no walking)
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false, false));
        // Prevent jumping
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false, false));
    }

    /**
     * Remove cinematic lock
     */
    private void removeCinematicLock() {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    public void playNode(DialogueNode node) {
        if (node == null) {
            end();
            return;
        }
        cleanupChoices();
        cancelAllTasks();
        this.currentNode = node;
        this.displayingOptions = false;
        this.player.getInventory().clear();

        // Execute Actions (Instant)
        executeActions(node);

        // Execute Per-Node Rewards
        executeNodeRewards(node);

        // Face NPC if possible
        faceNPC();

        // Prepare dialogue text
        String rawText = node.getText().replace("%player%", player.getName());

        // Strip color codes for word splitting (keep original for display)
        fullDialogueText = rawText;
        revealedWords = 0;
        typewriterDone = false;

        // Initialize Hologram
        setupMainHologram();

        // Start typewriter effect on ActionBar & Hologram
        startTypewriterEffect();

        // BossBar & Timer
        setupTimer(node);
    }

    /**
     * ActionBar Typewriter Effect
     * Reveals text word by word with typing sound
     * Format: [unicode] text...
     */
    private void startTypewriterEffect() {
        String[] words = stripColors(fullDialogueText).split(" ");
        int totalWords = words.length;

        // Get unicode background from interaction config
        String unicode = interaction.getDialogueUnicode();
        String prefix = unicode.isEmpty() ? "" : unicode + " ";

        typewriterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                revealedWords++;

                if (revealedWords >= totalWords) {
                    revealedWords = totalWords;
                    typewriterDone = true;
                    if (!displayingOptions)
                        displayHotbarOptions();
                    cancel();
                }

                // Build the revealed portion from the ORIGINAL colored text
                String revealed = getRevealedText(fullDialogueText, revealedWords);

                // Add space padding to stabilize center alignment
                int missingChars = stripColors(fullDialogueText).length() - stripColors(revealed).length();
                String padding = " ".repeat(Math.max(0, missingChars));

                Component actionBarText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                        .toComponent(prefix + revealed + padding);

                player.sendActionBar(actionBarText);
                updateMainHologram(revealed);

                // Typing sound effect
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
            }
        };
        typewriterTask.runTaskTimer(plugin, 0L, 3L); // Every 3 ticks (~150ms per word)

        // Keep ActionBar alive after typewriter finishes (ActionBar fades after 2 sec)
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (typewriterDone) {
                    String revealed = getRevealedText(fullDialogueText, revealedWords);

                    int missingChars = stripColors(fullDialogueText).length() - stripColors(revealed).length();
                    String padding = " ".repeat(Math.max(0, missingChars));

                    Component actionBarText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                            .toComponent(prefix + revealed + padding);
                    player.sendActionBar(actionBarText);
                    updateMainHologram(revealed);
                }
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 20L); // Refresh every second to keep it visible
    }

    /**
     * Get the first N words from a colored text string, preserving color codes
     */
    private String getRevealedText(String coloredText, int wordCount) {
        String[] words = coloredText.split(" ");
        if (wordCount >= words.length)
            return coloredText;

        StringBuilder result = new StringBuilder();
        String activeColor = "";
        for (int i = 0; i < wordCount; i++) {
            if (i > 0)
                result.append(" ");

            // Only prepend active color if this word doesn't explicitly start with a color
            // reset or distinct color
            if (!words[i].startsWith("&") && !words[i].startsWith("§")) {
                result.append(activeColor);
            }
            result.append(words[i]);
            activeColor = getActiveColors(result.toString());
        }
        return result.toString();
    }

    private String getActiveColors(String text) {
        String active = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (c == '&' && next == '#' && i + 8 < text.length()) {
                    active = text.substring(i, i + 9);
                    i += 8;
                } else if ("0123456789abcdefABCDEF".indexOf(next) != -1) {
                    active = text.substring(i, i + 2);
                    i++;
                } else if ("klmnorKLMNOR".indexOf(next) != -1) {
                    if (next == 'r' || next == 'R')
                        active = text.substring(i, i + 2);
                    else
                        active += text.substring(i, i + 2);
                    i++;
                }
            }
        }
        return active;
    }

    /**
     * Strip color codes for counting purposes
     */
    private String stripColors(String text) {
        String result = text.replaceAll("&#[A-Fa-f0-9]{6}", "");
        result = result.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        result = result.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        result = result.replaceAll("<[^>]+>", "");
        return result;
    }

    /**
     * Obsolete chat clearing method (removed body)
     */
    private void clearChat() {
        // No longer using chat for options
    }

    /**
     * Set up the single main hologram TextDisplay above NPC head for dialogue +
     * choices
     */
    private void setupMainHologram() {
        org.bukkit.Location npcBase = findNPCLocation();
        if (npcBase != null) {
            org.bukkit.Location loc = npcBase.clone().add(0, 2.8, 0);
            mainTextDisplay = loc.getWorld().spawn(loc, org.bukkit.entity.TextDisplay.class, td -> {
                td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                td.setPersistent(false);
                td.setViewRange(15);
                td.setBackgroundColor(org.bukkit.Color.fromARGB(160, 0, 0, 0));
                td.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
            });
        }
    }

    /**
     * Update hologram text with revealed dialogue and options when ready
     */
    private void updateMainHologram(String revealedText) {
        if (mainTextDisplay == null || !mainTextDisplay.isValid())
            return;

        StringBuilder content = new StringBuilder();
        content.append(revealedText);

        // Show options once typewriter is done
        if (typewriterDone && currentNode != null && !currentNode.getOptions().isEmpty()) {
            content.append("\n");
            for (int i = 0; i < currentNode.getOptions().size(); i++) {
                Option opt = currentNode.getOptions().get(i);
                content.append("\n&#FFAA00&l").append(i + 1).append(" &#FFFF55>> &e").append(opt.getText());
            }
        }

        mainTextDisplay.text(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(content.toString().trim()));
    }

    private void displayHotbarOptions() {
        if (currentNode == null || currentNode.getOptions().isEmpty())
            return;

        // Letakkan kursor di tengah (slot 4) sebelum memunculkan opsi agar event tidak
        // me-cancel ini
        player.getInventory().setHeldItemSlot(4);
        displayingOptions = true;

        player.getInventory().clear();
        for (int i = 0; i < currentNode.getOptions().size(); i++) {
            if (i > 8)
                break;
            Option opt = currentNode.getOptions().get(i);
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.displayName(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&#FFAA00&lPilih: &f" + opt.getText()));
            item.setItemMeta(meta);
            // Tambahkan item dari kiri ke kanan (slot 0, 1, 2...)
            player.getInventory().setItem(i, item);
        }
    }

    public boolean isDisplayingOptions() {
        return displayingOptions;
    }

    /**
     * Find the NPC location for this interaction
     */
    private org.bukkit.Location findNPCLocation() {
        try {
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (npc.isSpawned() && npc.getStoredLocation().getWorld().equals(player.getWorld())) {
                    if (npc.getStoredLocation().distanceSquared(player.getLocation()) < 25) {
                        if (npc.hasTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)) {
                            String id = npc.getTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)
                                    .getInteractionId();
                            if (interaction.getId().equals(id)) {
                                return npc.getStoredLocation();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Setup the timer BossBar and auto-advance/timeout logic
     */
    private void setupTimer(DialogueNode node) {
        if (bossBar != null)
            player.hideBossBar(bossBar);

        // Cinematic boss bar with interaction name
        String npcName = interaction.getId().replace("_", " ");
        npcName = npcName.substring(0, 1).toUpperCase() + npcName.substring(1);
        bossBar = BossBar.bossBar(
                Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(npcName, NamedTextColor.YELLOW))
                        .append(Component.text(" ✦", NamedTextColor.GOLD)),
                1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        final int durationTicks = node.getDurationSeconds() * 20;

        timerTask = new BukkitRunnable() {
            int ticksLeft = durationTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    plugin.getInteractionManager().endInteraction(player.getUniqueId());
                    return;
                }

                ticksLeft -= 2;
                float progress = durationTicks > 0 ? (float) ticksLeft / durationTicks : 0;
                if (progress < 0)
                    progress = 0;
                bossBar.progress(progress);

                if (ticksLeft <= 0) {
                    handleTimeout();
                    cancel();
                }
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 2L);
    }

    private void handleTimeout() {
        if (currentNode.getOptions().isEmpty() && currentNode.getNextNodeId() != null) {
            // Auto advance
            playNode(interaction.getNode(currentNode.getNextNodeId()));
        } else if (currentNode.getOptions().isEmpty()) {
            // End of conversation
            end();
        } else {
            // Options exist but time ran out
            player.sendMessage(Component.text("⏱ Waktu habis.", NamedTextColor.RED));
            end();
        }
    }

    private void executeActions(DialogueNode node) {
        for (Action action : node.getActions()) {
            id.naturalsmp.naturalinteraction.utils.ActionExecutor.execute(player, action);
        }
    }

    private void faceNPC() {
        try {
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (npc.isSpawned() && npc.getStoredLocation().getWorld().equals(player.getWorld())) {
                    if (npc.getStoredLocation().distanceSquared(player.getLocation()) < 25) {
                        if (npc.hasTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)) {
                            String id = npc.getTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)
                                    .getInteractionId();
                            if (interaction.getId().equals(id)) {
                                // Move player to cinematic distance (2.5 blocks)
                                org.bukkit.Location npcLoc = npc.getStoredLocation().clone();
                                org.bukkit.Location playerLoc = player.getLocation();

                                org.bukkit.util.Vector dir = playerLoc.toVector().subtract(npcLoc.toVector())
                                        .normalize();
                                if (dir.lengthSquared() == 0 || Double.isNaN(dir.getX())) {
                                    dir = npcLoc.getDirection().multiply(-1);
                                }

                                org.bukkit.Location targetLoc = npcLoc.clone().add(dir.multiply(2.5));
                                targetLoc.setY(playerLoc.getY());

                                org.bukkit.Location lookLoc = npcLoc.clone().add(0, 1.5, 0);
                                targetLoc.setDirection(lookLoc.toVector().subtract(targetLoc.toVector()).normalize());

                                player.teleport(targetLoc,
                                        org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Citizens not loaded, skip
        }
    }

    public void skip() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("Interaksi ini telah kedaluwarsa.", NamedTextColor.RED));
            return;
        }

        // If typewriter is still running, first complete it instantly
        if (!typewriterDone) {
            typewriterDone = true;
            revealedWords = stripColors(fullDialogueText).split(" ").length;
            if (typewriterTask != null && !typewriterTask.isCancelled())
                typewriterTask.cancel();

            if (!displayingOptions)
                displayHotbarOptions();

            // Show full text immediately with padding
            String unicode = interaction.getDialogueUnicode();
            String prefix = unicode.isEmpty() ? "" : unicode + " ";
            Component fullText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent(prefix + fullDialogueText);
            player.sendActionBar(fullText);
            updateMainHologram(fullDialogueText);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
            return; // First skip = complete typewriter. Second skip = advance.
        }

        // Skip timer and advance
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
        }
        if (bossBar != null)
            bossBar.progress(0);
        handleTimeout();
    }

    public void selectOption(Option option) {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("Interaksi ini telah kedaluwarsa.", NamedTextColor.RED));
            return;
        }

        // Selection sound
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);

        cancelAllTasks();
        playNode(interaction.getNode(option.getTargetNodeId()));
    }

    public void selectOptionByIndex(int index) {
        if (currentNode == null || index < 0 || index >= currentNode.getOptions().size())
            return;
        selectOption(currentNode.getOptions().get(index));
    }

    public void end() {
        if (bossBar != null)
            player.hideBossBar(bossBar);
        cancelAllTasks();
        cleanupChoices();
        removeCinematicLock();

        // Safety: remove invisibility if it was set during interaction
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        player.setInvisible(false);

        // Restore inventory
        if (originalInventory != null) {
            player.getInventory().setContents(originalInventory);
        }

        plugin.getInteractionManager().endInteraction(player.getUniqueId());

        // Clear the ActionBar
        player.sendActionBar(Component.empty());

        // Ending sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

        // Give Rewards (with one-time check)
        CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();
        boolean alreadyCompleted = tracker.hasCompleted(player.getUniqueId(), interaction.getId());
        boolean shouldGiveReward = !interaction.isOneTimeReward() || !alreadyCompleted;
        boolean given = false;

        if (shouldGiveReward) {
            // Item Rewards
            if (!interaction.getRewards().isEmpty()) {
                for (org.bukkit.inventory.ItemStack item : interaction.getRewards()) {
                    if (item != null)
                        player.getInventory().addItem(item);
                }
                given = true;
            }

            // Command Rewards
            if (!interaction.getCommandRewards().isEmpty()) {
                org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
                for (String cmd : interaction.getCommandRewards()) {
                    String finalCmd = cmd.replace("%player_name%", player.getName());
                    Bukkit.dispatchCommand(console, finalCmd);
                }
                given = true;
            }

            if (given) {
                player.sendMessage(Component.text("✨ ", NamedTextColor.GOLD)
                        .append(Component.text("Kamu mendapatkan hadiah!", NamedTextColor.GREEN)));
            }
        } else if (alreadyCompleted) {
            player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                    .append(Component.text("Kamu sudah pernah menyelesaikan interaksi ini.", NamedTextColor.GRAY)));
        }

        // Mark as completed
        if (!alreadyCompleted) {
            tracker.markCompleted(player.getUniqueId(), interaction.getId());
        }

        // Apply Cooldown if interaction has it
        long cooldownSecs = interaction.getCooldownSeconds();
        if (cooldownSecs > 0) {
            plugin.getInteractionManager().setOnCooldown(player.getUniqueId(), interaction.getId(), cooldownSecs);
        }
    }

    private void cancelAllTasks() {
        if (timerTask != null && !timerTask.isCancelled())
            timerTask.cancel();
        if (typewriterTask != null && !typewriterTask.isCancelled())
            typewriterTask.cancel();
        if (actionBarTask != null && !actionBarTask.isCancelled())
            actionBarTask.cancel();
    }

    private void cleanupChoices() {
        for (org.bukkit.entity.Entity entity : choiceEntities) {
            if (entity.isValid())
                entity.remove();
        }
        choiceEntities.clear();

        if (mainTextDisplay != null && mainTextDisplay.isValid()) {
            mainTextDisplay.remove();
        }
    }

    /**
     * Execute per-node command rewards if configured
     */
    private void executeNodeRewards(DialogueNode node) {
        if (!node.isGiveReward() || node.getCommandRewards().isEmpty())
            return;

        if (interaction.isOneTimeReward()) {
            CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();
            if (tracker.hasCompleted(player.getUniqueId(), interaction.getId())) {
                return;
            }
        }

        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String cmd : node.getCommandRewards()) {
            String finalCmd = cmd.replace("%player_name%", player.getName());
            Bukkit.dispatchCommand(console, finalCmd);
        }
        player.sendMessage(Component.text("✨ ", NamedTextColor.GOLD)
                .append(Component.text("Hadiah node diterima!", NamedTextColor.GREEN)));
    }
}
