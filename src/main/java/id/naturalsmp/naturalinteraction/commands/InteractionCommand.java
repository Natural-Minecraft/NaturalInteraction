package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.StoryWand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InteractionCommand implements CommandExecutor {

    private final NaturalInteraction plugin;

    public InteractionCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.toComponent("<red>Usage: /ni start <nodeId>"));
                    return true;
                }
                plugin.getStoryManager().startStory(player, args[1]);
                player.sendMessage(ChatUtils.toComponent("<green>Story started: <gold>" + args[1]));
                break;
            case "editor":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                new AdminGUIEditor(plugin).openMainMenu(player);
                break;
            case "wand":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                player.getInventory().addItem(StoryWand.getWand());
                player.sendMessage(ChatUtils.toComponent("<green>You received the Story Creator Wand!"));
                break;
            case "reload":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                plugin.getStoryManager().loadNodes();
                player.sendMessage(ChatUtils.toComponent("<green>Story nodes reloaded!"));
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(
                ChatUtils.toComponent("<gradient:#4facfe:#00f2fe><b>--- NaturalInteraction ---</b></gradient>"));
        player.sendMessage(ChatUtils.toComponent("<gray>/ni start <nodeId> - Start a quest"));
        if (player.hasPermission("naturalinteraction.admin")) {
            player.sendMessage(ChatUtils.toComponent("<gray>/ni editor - Open story editor"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni wand - Get creator wand"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni reload - Reload config"));
        }
    }
}
