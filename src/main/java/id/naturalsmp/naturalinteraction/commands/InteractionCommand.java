package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.StoryWand;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.gui.InteractionEditorGUI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
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
                new id.naturalsmp.naturalinteraction.gui.AdminEditorGUI(plugin, player).open();
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
            case "create":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /ni create <name>");
                    return true;
                }
                String name = args[1];
                if (plugin.getInteractionManager().hasInteraction(name)) {
                    player.sendMessage(ChatColor.RED + "Interaction '" + name + "' already exists.");
                    return true;
                }
                plugin.getInteractionManager().createInteraction(name);
                player.sendMessage(ChatColor.GREEN + "Created interaction '" + name + "'. Opening editor...");
                new InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(name)).open();
                break;
            case "play":
                if (args.length < 2) {
                    NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
                    if (npc != null && npc.hasTrait(InteractionTrait.class)) {
                        String id = npc.getTrait(InteractionTrait.class).getInteractionId();
                        if (id != null) {
                            plugin.getInteractionManager().startInteraction(player, id);
                            return true;
                        }
                    }
                    player.sendMessage(ChatColor.RED + "Usage: /ni play <name>");
                    return true;
                }
                plugin.getInteractionManager().startInteraction(player, args[1]);
                break;
            case "bind":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /ni bind <name>");
                    return true;
                }
                NPC targetNpc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
                if (targetNpc == null) {
                    player.sendMessage(ChatColor.RED + "You must have an NPC selected.");
                    return true;
                }
                if (!targetNpc.hasTrait(InteractionTrait.class)) {
                    targetNpc.addTrait(InteractionTrait.class);
                }
                targetNpc.getTrait(InteractionTrait.class).setInteractionId(args[1]);
                player.sendMessage(
                        ChatColor.GREEN + "Bound interaction '" + args[1] + "' to NPC " + targetNpc.getName());
                break;
            case "edit":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /ni edit <name>");
                    return true;
                }
                if (!plugin.getInteractionManager().hasInteraction(args[1])) {
                    player.sendMessage(ChatColor.RED + "Interaction '" + args[1] + "' does not exist.");
                    return true;
                }
                new InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(args[1])).open();
                break;
            case "delete":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /ni delete <name>");
                    return true;
                }
                plugin.getInteractionManager().deleteInteraction(args[1]);
                player.sendMessage(ChatColor.GREEN + "Deleted interaction '" + args[1] + "'.");
                break;
            case "spawnfish":
                if (!player.hasPermission("naturalinteraction.admin"))
                    return true;
                id.naturalsmp.naturalinteraction.utils.LocationUtils.ShorelineResult shoreRes = id.naturalsmp.naturalinteraction.utils.LocationUtils
                        .findSafeShoreline(player.getLocation(), 32);
                if (shoreRes == null) {
                    player.sendMessage(ChatUtils.toComponent("<red>Tidak menemukan pinggiran air di sekitarmu!"));
                    return true;
                }

                org.bukkit.Location land = shoreRes.landLoc;
                org.bukkit.Location water = shoreRes.waterLoc;

                // Content-Aware Decoration: Place a Barrel and a Lantern nearby
                land.clone().add(1, 0, 0).getBlock().setType(org.bukkit.Material.BARREL);
                land.clone().add(0, 0, 1).getBlock().setType(org.bukkit.Material.LANTERN);

                NPC kakek = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, "Kakek Tua");
                kakek.spawn(land);

                // Make NPC face water
                kakek.faceLocation(water);

                if (!kakek.hasTrait(InteractionTrait.class)) {
                    kakek.addTrait(InteractionTrait.class);
                }
                kakek.getTrait(InteractionTrait.class).setInteractionId("fishing_story");
                player.sendMessage(ChatUtils.toComponent(
                        "<green>Kakek Tua telah muncul di pinggir air dengan perlengkapannya! Cek sekitarmu."));
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
        player.sendMessage(ChatUtils.toComponent("<gray>/ni play <name> - Play dialogue interaction"));
        if (player.hasPermission("naturalinteraction.admin")) {
            player.sendMessage(ChatUtils.toComponent("<yellow>--- Quest/Story Editor ---"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni editor - Open story editor"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni wand - Get creator wand"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni reload - Reload config"));
            player.sendMessage(ChatUtils.toComponent("<yellow>--- Dialogue Editor ---"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni create <name> - Create dialogue"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni edit <name> - Edit dialogue"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni bind <name> - Bind dialogue to NPC"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni delete <name> - Delete dialogue"));
            player.sendMessage(ChatUtils.toComponent("<gray>/ni spawnfish - Dynamic spawn Fishing Story NPC"));
        }
    }
}
