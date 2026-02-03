package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.gui.InteractionEditorGUI;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public EditCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "edit";
    }

    @Override
    public String getDescription() {
        return "Edit an existing dialogue interaction.";
    }

    @Override
    public String getUsage() {
        return "/ni edit <name>";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return;

        if (args.length < 2) {
            player.sendMessage(ChatUtils.toComponent("<red>Usage: " + getUsage()));
            return;
        }

        if (!plugin.getInteractionManager().hasInteraction(args[1])) {
            player.sendMessage(ChatUtils.toComponent("<red>Interaction '" + args[1] + "' does not exist."));
            return;
        }

        new InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(args[1])).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase();
            return plugin.getInteractionManager().getInteractionIds().stream()
                    .filter(id -> id.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
