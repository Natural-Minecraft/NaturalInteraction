package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.subs.*;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class InteractionCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public InteractionCommand(NaturalInteraction plugin) {
        registerSubCommand(new StartCommand(plugin));
        registerSubCommand(new EditorCommand(plugin));
        registerSubCommand(new WandCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new CreateCommand(plugin));
        registerSubCommand(new PlayCommand(plugin));
        registerSubCommand(new BindCommand(plugin));
        registerSubCommand(new EditCommand(plugin));
        registerSubCommand(new DeleteCommand(plugin));
        registerSubCommand(new SpawnFishCommand(plugin));
    }

    private void registerSubCommand(SubCommand sub) {
        subCommands.put(sub.getName().toLowerCase(), sub);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            sendHelp(sender);
            return true;
        }

        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(ChatUtils.toComponent("<red>You don't have permission to use this command."));
            return true;
        }

        sub.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return subCommands.values().stream()
                    .filter(sub -> sub.getPermission() == null || sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.onTabComplete(sender, args);
            }
        }

        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
                ChatUtils.toComponent("<gradient:#4facfe:#00f2fe><b>--- NaturalInteraction ---</b></gradient>"));
        subCommands.values().stream()
                .filter(sub -> sub.getPermission() == null || sender.hasPermission(sub.getPermission()))
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(sub -> sender
                        .sendMessage(ChatUtils.toComponent("<gray>" + sub.getUsage() + " - " + sub.getDescription())));
    }
}
