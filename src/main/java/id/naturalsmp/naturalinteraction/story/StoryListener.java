package id.naturalsmp.naturalinteraction.story;

import id.naturalsmp.naturalinteraction.story.StoryManager.PlayerStoryData;
import id.naturalsmp.naturalinteraction.story.StoryNode.StoryObjective;
import net.citizensnpcs.api.event.NPCClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class StoryListener implements Listener {

    private final StoryManager storyManager;

    public StoryListener(StoryManager storyManager) {
        this.storyManager = storyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Ensure player exists in system
        storyManager.getPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onNPCClick(NPCClickEvent event) {
        Player player = event.getClicker();
        StoryNode current = storyManager.getPlayerCurrentNode(player);
        if (current == null)
            return;

        List<StoryObjective> objectives = current.getObjectives();
        PlayerStoryData data = storyManager.getPlayerData(player);

        for (int i = 0; i < objectives.size(); i++) {
            StoryObjective obj = objectives.get(i);
            if (obj.getType() == ObjectiveType.TALK_TO_NPC) {
                if (obj.getTargetId().equals(String.valueOf(event.getNPC().getId())) ||
                        obj.getTargetId().equalsIgnoreCase(event.getNPC().getName())) {

                    data.setObjectiveProgress(i, obj.getRequiredAmount());
                    checkCompletion(player, current, data);
                }
            }
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        StoryNode current = storyManager.getPlayerCurrentNode(killer);
        if (current == null)
            return;

        List<StoryObjective> objectives = current.getObjectives();
        PlayerStoryData data = storyManager.getPlayerData(killer);

        for (int i = 0; i < objectives.size(); i++) {
            StoryObjective obj = objectives.get(i);
            if (obj.getType() == ObjectiveType.KILL_MOBS) {
                if (obj.getTargetId().equalsIgnoreCase(event.getEntityType().name())) {
                    int currentProg = data.getObjectiveProgress().getOrDefault(i, 0);
                    if (currentProg < obj.getRequiredAmount()) {
                        data.setObjectiveProgress(i, currentProg + 1);
                        checkCompletion(killer, current, data);
                    }
                }
            }
        }
    }

    private void checkCompletion(Player player, StoryNode node, PlayerStoryData data) {
        boolean allDone = true;
        List<StoryObjective> objectives = node.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (data.getObjectiveProgress().getOrDefault(i, 0) < objectives.get(i).getRequiredAmount()) {
                allDone = false;
                break;
            }
        }

        if (allDone) {
            storyManager.advanceStory(player);
        } else {
            storyManager.saveProgress();
        }
    }
}
