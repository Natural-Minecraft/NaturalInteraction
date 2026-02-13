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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class InteractionSession {
    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;
    private DialogueNode currentNode;
    private BukkitRunnable task;
    private BossBar bossBar;
    private final java.util.List<org.bukkit.entity.Entity> choiceEntities = new java.util.ArrayList<>();

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
        playNode(node);
    }

    public void playNode(DialogueNode node) {
        if (node == null) {
            end();
            return;
        }
        cleanupChoices();
        this.currentNode = node;

        // Execute Actions (Instant)
        executeActions(node);

        // Execute Per-Node Rewards
        executeNodeRewards(node);

        // Typewriter Sound Effect (Cinematic)
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_STEP, 1.0f, 1.5f);

        // Face NPC if possible
        faceNPC();

        // Cinematic Dialogue: Titles and Subtitles
        // Use Typewriter effect concept: Show in chat AND as Subtitle
        String rawText = node.getText();
        Component coloredText = id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(rawText);

        // Separator (Console/Chat Log style)
        player.sendMessage(
                Component.text("-----------------------------------------------------", NamedTextColor.GRAY));

        // NPC Identity
        String npcName = interaction.getId(); // Default to ID
        // In the future, we can bound this to a Citizens NPC name

        Component npcPrefix = Component.text("[NPC] ", NamedTextColor.YELLOW)
                .append(Component.text(npcName, NamedTextColor.GOLD))
                .append(Component.text(": ", NamedTextColor.WHITE));

        Component chatMessage = npcPrefix.append(coloredText);

        // Add Skip Button if skippable and has delay
        if (node.isSkippable() && (node.getDurationSeconds() > 0)) {
            chatMessage = chatMessage.append(Component
                    .text(" [KLIK UNTUK SKIP]", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .clickEvent(ClickEvent.callback(audience -> {
                        skip();
                    }))
                    .hoverEvent(
                            HoverEvent.showText(Component.text("Klik untuk lewati dialog", NamedTextColor.YELLOW))));
        }
        player.sendMessage(chatMessage);
        player.sendMessage(Component.empty());

        // SHOW TITLE (Cinematic)
        String[] words = rawText.split(" ");
        Component titleComponent;
        Component subtitleComponent = Component.empty();

        if (words.length > 5) {
            StringBuilder titleBuilder = new StringBuilder();
            StringBuilder subtitleBuilder = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i < 5) {
                    titleBuilder.append(words[i]).append(" ");
                } else {
                    subtitleBuilder.append(words[i]).append(" ");
                }
            }
            titleComponent = id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent(titleBuilder.toString().trim());
            subtitleComponent = id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent(subtitleBuilder.toString().trim());
        } else {
            titleComponent = coloredText;
        }

        player.showTitle(net.kyori.adventure.title.Title.title(
                titleComponent,
                subtitleComponent,
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(200),
                        java.time.Duration.ofMillis(3000), java.time.Duration.ofMillis(500))));
        player.sendMessage(Component.text(""));

        // Display Options
        if (!node.getOptions().isEmpty()) {
            player.sendMessage(Component.text("   Pilih opsi:", NamedTextColor.GRAY));

            // Spawn TextDisplays for visual choice (v1.19.4+)
            spawnVisualChoices(node);

            for (Option option : node.getOptions()) {
                Component optionText = Component.text("   ➤ ", NamedTextColor.GOLD)
                        .append(Component.text(option.getText(), NamedTextColor.YELLOW,
                                net.kyori.adventure.text.format.TextDecoration.BOLD))
                        .clickEvent(ClickEvent.callback(audience -> {
                            selectOption(option);
                        }))
                        .hoverEvent(HoverEvent
                                .showText(Component.text("Klik untuk memilih opsi ini", NamedTextColor.GREEN)));
                player.sendMessage(optionText);
            }
            player.sendMessage(Component.empty());
        }

        player.sendMessage(
                Component.text("-----------------------------------------------------", NamedTextColor.GRAY));

        // BossBar & Timer
        if (bossBar != null)
            player.hideBossBar(bossBar);
        bossBar = BossBar.bossBar(Component.text("Duration"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        if (task != null && !task.isCancelled())
            task.cancel();

        final int durationTicks = node.getDurationSeconds() * 20;

        task = new BukkitRunnable() {
            int ticksLeft = durationTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    plugin.getInteractionManager().endInteraction(player.getUniqueId());
                    return;
                }

                ticksLeft -= 2; // Run every 2 ticks
                // Prevent division by zero if duration is 0
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
        task.runTaskTimer(plugin, 0L, 2L);
    }

    private void handleTimeout() {
        if (currentNode.getOptions().isEmpty() && currentNode.getNextNodeId() != null) {
            // Auto advance
            playNode(interaction.getNode(currentNode.getNextNodeId()));
        } else if (currentNode.getOptions().isEmpty()) {
            // End of conversation
            end();
        } else {
            // Options exist but time ran out. End.
            player.sendMessage(Component.text("Time expired.", NamedTextColor.RED));
            end();
        }
    }

    private void executeActions(DialogueNode node) {
        for (Action action : node.getActions()) {
            id.naturalsmp.naturalinteraction.utils.ActionExecutor.execute(player, action);
        }
    }

    private void faceNPC() {
        // Try to find if this interaction is triggered by an NPC
        // For now, we look for nearby Citizens NPCs with this interaction ID
        for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
            if (npc.isSpawned() && npc.getStoredLocation().getWorld().equals(player.getWorld())) {
                if (npc.getStoredLocation().distanceSquared(player.getLocation()) < 25) { // Within 5 blocks
                    if (npc.hasTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)) {
                        String id = npc.getTrait(id.naturalsmp.naturalinteraction.hook.InteractionTrait.class)
                                .getInteractionId();
                        if (interaction.getId().equals(id)) {
                            // Move player to cinematic distance (2.5 blocks)
                            org.bukkit.Location npcLoc = npc.getStoredLocation().clone();
                            org.bukkit.Location playerLoc = player.getLocation();

                            // Calculate direction from NPC to Player
                            org.bukkit.util.Vector dir = playerLoc.toVector().subtract(npcLoc.toVector()).normalize();
                            if (dir.lengthSquared() == 0 || Double.isNaN(dir.getX())) {
                                dir = npcLoc.getDirection().multiply(-1); // Fallback to NPC back direction
                            }

                            org.bukkit.Location targetLoc = npcLoc.clone().add(dir.multiply(2.5));

                            // Adjust Y to find safe ground if possible
                            targetLoc.setY(playerLoc.getY());

                            // Look at NPC head
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
    }

    public void skip() {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("This interaction has expired.", NamedTextColor.RED));
            return;
        }

        // Skip current delay
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        bossBar.progress(0);
        handleTimeout();
    }

    public void selectOption(Option option) {
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != this) {
            player.sendMessage(Component.text("Interaksi ini telah kedaluwarsa.", NamedTextColor.RED));
            return;
        }

        if (task != null)
            task.cancel();
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
        if (task != null)
            task.cancel();
        cleanupChoices();
        plugin.getInteractionManager().endInteraction(player.getUniqueId());

        // Remove Zoom if active (cleanup)
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);

        player.sendMessage(Component.text("Interaction ended.", NamedTextColor.GRAY));

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
                player.sendMessage(Component.text("Kamu mendapatkan hadiah!", NamedTextColor.GREEN));
            }
        } else if (alreadyCompleted) {
            player.sendMessage(Component.text("Kamu sudah pernah menyelesaikan interaksi ini.", NamedTextColor.YELLOW));
        }

        // Mark as completed (always, for tracking)
        if (!alreadyCompleted) {
            tracker.markCompleted(player.getUniqueId(), interaction.getId());
        }
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

        // Check one-time: if interaction is one-time reward and already completed, skip
        // node rewards too
        if (interaction.isOneTimeReward()) {
            CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();
            if (tracker.hasCompleted(player.getUniqueId(), interaction.getId())) {
                return; // Don't give per-node rewards if already completed
            }
        }

        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String cmd : node.getCommandRewards()) {
            String finalCmd = cmd.replace("%player_name%", player.getName());
            Bukkit.dispatchCommand(console, finalCmd);
        }
        player.sendMessage(Component.text("✨ Hadiah node diterima!", NamedTextColor.GREEN));
    }

    private void spawnVisualChoices(DialogueNode node) {
        // Try to find the NPC location
        org.bukkit.Location npcBase = null;
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

        if (npcBase == null)
            return;

        double startHeight = 2.5; // Start above NPC head
        double spacing = 0.4;

        for (int i = 0; i < node.getOptions().size(); i++) {
            final int index = i;
            Option option = node.getOptions().get(i);
            org.bukkit.Location loc = npcBase.clone().add(0, startHeight + (i * spacing), 0);

            // 1. Spawn TextDisplay
            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc, org.bukkit.entity.TextDisplay.class,
                    td -> {
                        td.text(id.naturalsmp.naturalinteraction.utils.ChatUtils
                                .toComponent("&#FFAA00&l➤ &e" + option.getText()));
                        td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                        td.setPersistent(false);
                        td.setViewRange(15);
                    });
            choiceEntities.add(textDisplay);

            // 2. Spawn Interaction Entity (Floating Click Zone)
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
