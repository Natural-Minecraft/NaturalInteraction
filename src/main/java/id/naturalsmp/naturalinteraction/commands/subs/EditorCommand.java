package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EditorCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public EditorCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "editor";
    }

    @Override
    public String getDescription() {
        return "Toggle Hotbar Editor Mode.";
    }

    @Override
    public String getUsage() {
        return "/ni editor";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        // Toggle editor mode
        if (plugin.getEditorMode().isInEditorMode(player)) {
            plugin.getEditorMode().exitEditorMode(player);
        } else {
            plugin.getEditorMode().enterEditorMode(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
