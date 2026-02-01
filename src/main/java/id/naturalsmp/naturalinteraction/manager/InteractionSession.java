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

    public void playNode(DialogueNode node) {
        if (node == null) {
            end();
            return;
        }
        this.currentNode = node;
        
        // Execute Actions (Instant)
        executeActions(node);

        // Separator
        player.sendMessage(Component.text("-----------------------------------------------------", NamedTextColor.GRAY));
        
        // NPC Name and Message
        // Format: [NPC] Name: Message
        Component npcPrefix = Component.text("[NPC] ", NamedTextColor.YELLOW)
                .append(Component.text(interaction.getId(), NamedTextColor.GOLD)) // Or use real NPC name if bound
                .append(Component.text(": ", NamedTextColor.WHITE));
                
        Component message = npcPrefix.append(Component.text(node.getText(), NamedTextColor.WHITE));
        
        // Add Skip Button if skippable and has delay
        if (node.isSkippable() && (node.getDurationSeconds() > 0)) {
            message = message.append(Component.text(" [CLICK TO SKIP]", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .clickEvent(ClickEvent.callback(audience -> {
                        skip();
                    }))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to skip", NamedTextColor.YELLOW))));
        }
        player.sendMessage(message);
        player.sendMessage(Component.text("")); 

        // Display Options
        if (!node.getOptions().isEmpty()) {
            player.sendMessage(Component.text("   Select an option:", NamedTextColor.GRAY));
            for (Option option : node.getOptions()) {
                Component optionText = Component.text("   ➤ ", NamedTextColor.GOLD)
                        .append(Component.text(option.getText(), NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD))
                        .clickEvent(ClickEvent.callback(audience -> {
                             selectOption(option);
                        }))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to select this option", NamedTextColor.GREEN)));
                player.sendMessage(optionText);
            }
            player.sendMessage(Component.text(""));
        } else {
             // If no options, maybe a "Click to continue" hint or just wait?
             // Hypixel often has "Click to continue" for linear dialogues if not auto-advancing
        }
        
        player.sendMessage(Component.text("-----------------------------------------------------", NamedTextColor.GRAY));

        // BossBar & Timer
        if (bossBar != null) player.hideBossBar(bossBar);
        bossBar = BossBar.bossBar(Component.text("Duration"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        if (task != null && !task.isCancelled()) task.cancel();
        
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
                if (progress < 0) progress = 0;
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
            id.naturalsmp.naturalinteraction.util.ActionExecutor.execute(player, action);
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
            player.sendMessage(Component.text("This interaction has expired.", NamedTextColor.RED));
            return;
        }
        
        if (task != null) task.cancel();
        playNode(interaction.getNode(option.getTargetNodeId()));
    }

    public void end() {
        if (bossBar != null) player.hideBossBar(bossBar);
        if (task != null) task.cancel();
        plugin.getInteractionManager().endInteraction(player.getUniqueId());
        
        // Remove Zoom if active (cleanup)
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);

        player.sendMessage(Component.text("Interaction ended.", NamedTextColor.GRAY));
        
        // Give Rewards
        boolean given = false;
        
        // Item Rewards
        if (!interaction.getRewards().isEmpty()) {
            for (org.bukkit.inventory.ItemStack item : interaction.getRewards()) {
                if (item != null) player.getInventory().addItem(item);
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
             player.sendMessage(Component.text("You received rewards!", NamedTextColor.GREEN));
        }
    }
}