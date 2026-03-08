package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.CompletionTracker;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class StoryCommand implements CommandExecutor {

    private final NaturalInteraction plugin;

    // Story chapters in order (id -> display name)
    private static final Map<String, String> STORY_CHAPTERS = new LinkedHashMap<>();
    static {
        STORY_CHAPTERS.put("prologue", "&#FFD700✦ Prologue &7— Pertemuan dengan Dewi");
        STORY_CHAPTERS.put("alice", "&#87CEEB✦ Chapter 1 &7— Perpustakaan Alice");
        STORY_CHAPTERS.put("aveline", "&#FF69B4✦ Chapter 2 &7— Berkah Aveline");
    }

    public StoryCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();

        // Header
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.toComponent("&#4facfe&l═══ &f&lNATURAL STORY &#4facfe&l═══"));
        player.sendMessage(Component.empty());

        // Display each chapter status
        for (Map.Entry<String, String> entry : STORY_CHAPTERS.entrySet()) {
            String id = entry.getKey();
            String displayName = entry.getValue();
            boolean completed = tracker.hasCompleted(player.getUniqueId(), id);

            String status;
            if (completed) {
                status = " &a✔ Selesai";
            } else {
                // Check if it's the next available (first uncompleted)
                status = " &c✘ Belum Selesai";
            }

            player.sendMessage(ChatUtils.toComponent("  " + displayName + status));
        }

        player.sendMessage(Component.empty());

        // Count progress
        long completed = STORY_CHAPTERS.keySet().stream()
                .filter(id -> tracker.hasCompleted(player.getUniqueId(), id))
                .count();
        int total = STORY_CHAPTERS.size();

        String progressBar = buildProgressBar(completed, total);
        player.sendMessage(ChatUtils.toComponent("  &7Progress: " + progressBar + " &f" + completed + "/" + total));
        player.sendMessage(Component.empty());

        return true;
    }

    private String buildProgressBar(long completed, int total) {
        int barLength = 20;
        int filled = total > 0 ? (int) ((completed * barLength) / total) : 0;
        int empty = barLength - filled;

        StringBuilder bar = new StringBuilder("&#4facfe");
        for (int i = 0; i < filled; i++)
            bar.append("█");
        bar.append("&8");
        for (int i = 0; i < empty; i++)
            bar.append("░");

        return bar.toString();
    }
}
