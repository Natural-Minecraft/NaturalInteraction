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

        // Clear chat for cinematic focus
        clearChat();

        // Start typewriter effect on ActionBar
        startTypewriterEffect();

        // Display Options in chat (clickable) after a small delay
        if (!node.getOptions().isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                displayOptions(node);
            }, 10L); // Half-second delay so player reads the text first
        }

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
                    cancel();
                }

                // Build the revealed portion from the ORIGINAL colored text
                String revealed = getRevealedText(fullDialogueText, revealedWords);
                Component actionBarText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                        .toComponent(prefix + revealed);

                player.sendActionBar(actionBarText);

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
                    Component actionBarText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                            .toComponent(prefix + revealed);
                    player.sendActionBar(actionBarText);
                }
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 20L); // Refresh every second to keep it visible
    }

    /**
     * Get the first N words from a colored text string, preserving color codes
     */
    private String getRevealedText(String coloredText, int wordCount) {
        if (wordCount <= 0)
            return "";

        StringBuilder result = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();
        String lastColorCode = "";
        int wordsFound = 0;
        boolean inColorCode = false;
        int i = 0;

        while (i < coloredText.length() && wordsFound < wordCount) {
            char c = coloredText.charAt(i);

            // Check for & color codes (legacy)
            if (c == '&' && i + 1 < coloredText.length()) {
                char next = coloredText.charAt(i + 1);
                if (next == '#' && i + 8 < coloredText.length()) {
                    // Hex color: &#RRGGBB
                    String hexCode = coloredText.substring(i, i + 9);
                    lastColorCode = hexCode;
                    currentWord.append(hexCode);
                    i += 9;
                    continue;
                } else if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(next) != -1) {
                    String code = "&" + next;
                    lastColorCode = code;
                    currentWord.append(code);
                    i += 2;
                    continue;
                }
            }

            // Check for § color codes (section symbol)
            if (c == '§' && i + 1 < coloredText.length()) {
                char next = coloredText.charAt(i + 1);
                String code = "§" + next;
                lastColorCode = code;
                currentWord.append(code);
                i += 2;
                continue;
            }

            if (c == ' ') {
                if (currentWord.length() > 0) {
                    if (result.length() > 0)
                        result.append(' ');
                    result.append(currentWord);
                    currentWord.setLength(0);
                    wordsFound++;
                }
                // Carry over last color code to next word
                if (!lastColorCode.isEmpty()) {
                    currentWord.append(lastColorCode);
                }
                i++;
                continue;
            }

            currentWord.append(c);
            i++;
        }

        // Don't forget the last word being built
        if (currentWord.length() > 0 && wordsFound < wordCount) {
            if (result.length() > 0)
                result.append(' ');
            result.append(currentWord);
        }

        return result.toString();
    }

    /**
     * Strip color codes for counting purposes
     */
    private String stripColors(String text) {
        // Remove &#RRGGBB
        String result = text.replaceAll("&#[A-Fa-f0-9]{6}", "");
        // Remove &X
        result = result.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        // Remove §X
        result = result.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        // Remove MiniMessage tags
        result = result.replaceAll("<[^>]+>", "");
        return result;
    }

    /**
     * Clear player's chat space for cinematic focus
     */
    private void clearChat() {
        for (int i = 0; i < 20; i++) {
            player.sendMessage(Component.empty());
        }
    }

    /**
     * Display dialogue options in chat (clickable)
     */
    private void displayOptions(DialogueNode node) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ╔═══════════════════════════╗", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  ║ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Pilih jawaban:", NamedTextColor.GRAY))
                .append(Component.text("            ║", NamedTextColor.DARK_GRAY)));

        // Spawn TextDisplays for visual choice above NPC head
        spawnVisualChoices(node);

        for (Option option : node.getOptions()) {
            Component optionLine = Component.text("  ║ ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("  ➤ ", NamedTextColor.GOLD))
                    .append(Component.text(stripColors(option.getText()), NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .clickEvent(ClickEvent.callback(audience -> {
                        selectOption(option);
                    }))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("✦ Klik untuk memilih", NamedTextColor.GREEN)));
            player.sendMessage(optionLine);
        }

        player.sendMessage(Component.text("  ╚═══════════════════════════╝", NamedTextColor.DARK_GRAY));

        // Guidance in ActionBar during options (combined with dialogue)
        // The actionBar task will keep showing the dialogue text
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
            if (typewriterTask != null && !typewriterTask.isCancelled())
                typewriterTask.cancel();

            // Show full text immediately
            String unicode = interaction.getDialogueUnicode();
            String prefix = unicode.isEmpty() ? "" : unicode + " ";
            Component fullText = id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent(prefix + fullDialogueText);
            player.sendActionBar(fullText);
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

    private void spawnVisualChoices(DialogueNode node) {
        org.bukkit.Location npcBase = null;
        try {
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (npc.isSpawned() && npc.getStoredLocation().getWorld().equals(player.getWorld())) {
                    if (npc.getStoredLocation().distanceSquared(player.getLocation()) < 25) {
                        if (npc.hasTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)) {
                            String id = npc.getTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)
                                    .getInteractionId();
                            if (interaction.getId().equals(id)) {
                                npcBase = npc.getStoredLocation();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return;
        }

        if (npcBase == null)
            return;

        double startHeight = 2.5;
        double spacing = 0.4;

        for (int i = 0; i < node.getOptions().size(); i++) {
            final int index = i;
            Option option = node.getOptions().get(i);
            org.bukkit.Location loc = npcBase.clone().add(0, startHeight + (i * spacing), 0);

            // TextDisplay with semi-transparent dark background
            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc, org.bukkit.entity.TextDisplay.class,
                    td -> {
                        td.text(id.naturalsmp.naturalinteraction.utils.ChatUtils
                                .toComponent("&#FFAA00&l➤ &e" + option.getText()));
                        td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                        td.setPersistent(false);
                        td.setViewRange(15);
                        // Semi-transparent black background for readability
                        td.setBackgroundColor(org.bukkit.Color.fromARGB(160, 0, 0, 0));
                    });
            choiceEntities.add(textDisplay);

            // Interaction Entity (Floating Click Zone)
            org.bukkit.entity.Interaction interactEntity = loc.getWorld().spawn(loc,
                    org.bukkit.entity.Interaction.class, ie -> {
                        ie.setInteractionHeight(0.3f);
                        ie.setInteractionWidth(2.0f);
                        ie.getPersistentDataContainer().set(
                                new org.bukkit.NamespacedKey(plugin, "choice_index"),
                                org.bukkit.persistence.PersistentDataType.INTEGER,
                                index);
                        ie.setPersistent(false);
                    });
            choiceEntities.add(interactEntity);
        }
    }
}
