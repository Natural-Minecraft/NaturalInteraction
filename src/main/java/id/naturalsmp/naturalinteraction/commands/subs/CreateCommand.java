package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.gui.InteractionEditorGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CreateCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public CreateCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a new dialogue interaction.";
    }

    @Override
    public String getUsage() {
        return "/ni create <name>";
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
            player.sendMessage(ChatColor.RED + "Usage: " + getUsage());
            return;
        }

        String name = args[1];
        if (plugin.getInteractionManager().hasInteraction(name)) {
            player.sendMessage(ChatColor.RED + "Interaction '" + name + "' already exists.");
            return;
        }

        plugin.getInteractionManager().createInteraction(name);
        player.sendMessage(ChatColor.GREEN + "Created interaction '" + name + "'. Opening editor...");
        new InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(name)).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
