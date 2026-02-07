package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.InteractionGenerator;
import id.naturalsmp.naturalinteraction.utils.InteractionTemplate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QuickStartCommand extends SubCommand {

    private final NaturalInteraction plugin;

    public QuickStartCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "quickstart";
    }

    @Override
    public String getDescription() {
        return "Generate an interaction from a template";
    }

    @Override
    public String getUsage() {
        return "/ni quickstart <id> <template>";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.toComponent("<red>Player only command."));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtils.toComponent("<red>Usage: " + getUsage()));
            player.sendMessage(ChatUtils.toComponent("<gray>Templates: <white>" +
                    Arrays.stream(InteractionTemplate.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "))));
            return;
        }

        String id = args[1].toLowerCase();
        String templateStr = args[2].toUpperCase();
        InteractionTemplate template;

        try {
            template = InteractionTemplate.valueOf(templateStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.toComponent("<red>Invalid Template! Available: <white>" +
                    Arrays.stream(InteractionTemplate.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "))));
            return;
        }

        InteractionGenerator generator = new InteractionGenerator(plugin);
        if (generator.generate(id, template, player)) {
            player.sendMessage(ChatUtils.toComponent("<green><b>SUCCESS!</b> <white>Interaction <yellow>" + id
                    + " <white>generated from <aqua>" + template.name() + "<white> template."));
            player.sendMessage(ChatUtils.toComponent(
                    "<gray>Edit configuration in <white>plugins/NaturalInteraction/interactions/" + id + ".json"));
        } else {
            player.sendMessage(ChatUtils
                    .toComponent("<red>Error! Interaction ID <yellow>" + id + " <red>already exists or file error."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3) {
            return Arrays.stream(InteractionTemplate.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
