package id.naturalsmp.naturalinteraction.commands.subs;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.SubCommand;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.LocationUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnFishCommand implements SubCommand {

    private final NaturalInteraction plugin;

    public SpawnFishCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "spawnfish";
    }

    @Override
    public String getDescription() {
        return "Dynamic spawn Fishing Story NPC near water.";
    }

    @Override
    public String getUsage() {
        return "/ni spawnfish";
    }

    @Override
    public String getPermission() {
        return "naturalinteraction.admin";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return;

        LocationUtils.ShorelineResult shoreRes = LocationUtils.findSafeShoreline(player.getLocation(), 32);
        if (shoreRes == null) {
            player.sendMessage(ChatUtils.toComponent("<red>Tidak menemukan pinggiran air di sekitarmu!"));
            return;
        }

        org.bukkit.Location land = shoreRes.landLoc;
        org.bukkit.Location water = shoreRes.waterLoc;

        // Content-Aware Decoration
        land.clone().add(1, 0, 0).getBlock().setType(Material.BARREL);
        land.clone().add(0, 0, 1).getBlock().setType(Material.LANTERN);

        NPC kakek = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Kakek Tua");
        kakek.spawn(land);
        kakek.faceLocation(water);

        if (!kakek.hasTrait(InteractionTrait.class)) {
            kakek.addTrait(InteractionTrait.class);
        }
        kakek.getTrait(InteractionTrait.class).setInteractionId("fishing_story");

        player.sendMessage(ChatUtils.toComponent("<green>Kakek Tua telah muncul di pinggir air! Cek sekitarmu."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
