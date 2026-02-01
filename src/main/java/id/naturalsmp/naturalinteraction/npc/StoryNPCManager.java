package id.naturalsmp.naturalinteraction.npc;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.story.StoryNode;
import id.naturalsmp.naturalinteraction.story.ObjectiveType;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoryNPCManager {

    private final NaturalInteraction plugin;
    private final Map<Integer, Location> spawnLocations = new HashMap<>();

    public StoryNPCManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        startCheckTask();
    }

    public void registerSpawnLocation(int npcId, Location loc) {
        spawnLocations.put(npcId, loc);
    }

    private void startCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkNPCsForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Check every 2 seconds
    }

    private void checkNPCsForPlayer(Player player) {
        StoryNode current = plugin.getStoryManager().getPlayerCurrentNode(player);
        if (current == null)
            return;

        for (StoryNode.StoryObjective obj : current.getObjectives()) {
            if (obj.getType() == ObjectiveType.TALK_TO_NPC) {
                try {
                    int npcId = Integer.parseInt(obj.getTargetId());
                    NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                    if (npc != null) {
                        Location loc = spawnLocations.get(npcId);
                        if (loc != null && loc.getWorld().equals(player.getWorld())) {
                            double dist = loc.distance(player.getLocation());
                            if (dist < 30) {
                                if (!npc.isSpawned()) {
                                    npc.spawn(loc);
                                }
                            } else {
                                // Optional: despawn if far, but Citizens is global.
                                // For true individual NPCs, we'd need a multi-registry or packet-based
                                // approach.
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}