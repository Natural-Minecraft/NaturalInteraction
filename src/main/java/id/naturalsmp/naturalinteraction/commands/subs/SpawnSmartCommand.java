package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnSmartCommand extends SubCommand {

    private final NaturalInteraction plugin;

    public SpawnSmartCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Smart spawn an entity on safe ground";
    }

    @Override
    public String getUsage() {
        return "/ni spawn smart <entity_type>";
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

        if (args.length < 3 || !args[1].equalsIgnoreCase("smart")) {
            player.sendMessage(ChatUtils.toComponent("<red>Usage: " + getUsage()));
            return;
        }

        String entityStr = args[2].toUpperCase();
        EntityType type;
        try {
            type = EntityType.valueOf(entityStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.toComponent("<red>Invalid Entity Type: <white>" + entityStr));
            return;
        }

        // Get target block (where player is looking)
        Location target = player.getTargetBlockExact(50) != null
                ? player.getTargetBlockExact(50).getLocation().add(0, 1, 0)
                : player.getLocation();

        // Find safe ground
        Location safeGround = LocationUtils.findSafeGround(target, 5);

        if (safeGround == null) {
            player.sendMessage(ChatUtils.toComponent("<red>Could not find safe ground nearby."));
            return;
        }

        // Spawn the entity
        player.getWorld().spawnEntity(safeGround, type);
        player.sendMessage(
                ChatUtils.toComponent("<green>Spawned <yellow>" + type.name() + " <green>at safe location!"));
        player.sendMessage(ChatUtils.toComponent("<gray>Location: " +
                String.format("%.1f, %.1f, %.1f", safeGround.getX(), safeGround.getY(), safeGround.getZ())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("smart");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("smart")) {
            String input = args[2].toUpperCase();
            return Arrays.stream(EntityType.values())
                    .filter(EntityType::isSpawnable)
                    .map(Enum::name)
                    .filter(name -> name.startsWith(input))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
