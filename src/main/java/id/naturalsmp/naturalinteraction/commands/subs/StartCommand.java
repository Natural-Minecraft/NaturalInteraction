package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StartCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public StartCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Start a quest or dialogue interaction.";
    }

    @Override
    public String getUsage() {
        return "/ni start <id>";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.player";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatUtils.toComponent("<red>Usage: " + getUsage()));
            return;
        }

        String id = args[1];

        // Intelligently check if it's a dialogue or a story node
        if (plugin.getInteractionManager().hasInteraction(id)) {
            plugin.getInteractionManager().startInteraction(player, id);
        } else {
            // Assume it's a story node
            plugin.getStoryManager().startStory(player, id);
            player.sendMessage(ChatUtils.toComponent("<green>Story started: <gold>" + id));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            String input = args[1].toLowerCase();

            // Interaction IDs
            for (String id : plugin.getInteractionManager().getInteractionIds()) {
                if (id.toLowerCase().startsWith(input))
                    suggestions.add(id);
            }

            // Story Node IDs
            for (String id : plugin.getStoryManager().getStoryNodeIds()) {
                if (id.toLowerCase().startsWith(input))
                    suggestions.add(id);
            }

            return suggestions;
        }
        return new ArrayList<>();
    }
}
