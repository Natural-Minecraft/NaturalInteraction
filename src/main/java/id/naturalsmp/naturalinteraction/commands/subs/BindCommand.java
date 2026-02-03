package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BindCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public BindCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "bind";
    }

    @Override
    public String getDescription() {
        return "Bind an interaction to a selected NPC.";
    }

    @Override
    public String getUsage() {
        return "/ni bind <name>";
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

        NPC targetNpc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
        if (targetNpc == null) {
            player.sendMessage(ChatColor.RED + "You must have an NPC selected.");
            return;
        }

        if (!targetNpc.hasTrait(InteractionTrait.class)) {
            targetNpc.addTrait(InteractionTrait.class);
        }

        targetNpc.getTrait(InteractionTrait.class).setInteractionId(args[1]);
        player.sendMessage(ChatColor.GREEN + "Bound interaction '" + args[1] + "' to NPC " + targetNpc.getName());
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
