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

public class PlayCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public PlayCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getDescription() {
        return "Play a dialogue interaction.";
    }

    @Override
    public String getUsage() {
        return "/ni play [name]";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.player";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return;

        if (args.length < 2) {
            NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
            if (npc != null && npc.hasTrait(InteractionTrait.class)) {
                String id = npc.getTrait(InteractionTrait.class).getInteractionId();
                if (id != null) {
                    plugin.getInteractionManager().startInteraction(player, id);
                    return;
                }
            }
            player.sendMessage(ChatColor.RED + "Usage: " + getUsage());
            return;
        }

        plugin.getInteractionManager().startInteraction(player, args[1]);
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
