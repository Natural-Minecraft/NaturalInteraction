package id.naturalsmp.naturalinteraction.commands;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface SubCommand {
    String getName();

    String getDescription();

    String getUsage();

    String getPermission();

    void execute(CommandSender sender, String[] args);

    List<String> onTabComplete(CommandSender sender, String[] args);
}
