package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryNode;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NaturalInteractionExpansion extends PlaceholderExpansion {

    private final NaturalInteraction plugin;

    public NaturalInteractionExpansion(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "naturalinteraction";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NaturalSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline())
            return "";
        Player p = player.getPlayer();

        // %naturalinteraction_current_title%
        if (params.equalsIgnoreCase("current_title")) {
            StoryNode node = plugin.getStoryManager().getPlayerCurrentNode(p);
            return node != null ? ChatUtils.serialize(ChatUtils.toComponent(node.getTitle())) : "No Quest";
        }

        // %naturalinteraction_current_objective%
        if (params.equalsIgnoreCase("current_objective")) {
            StoryNode node = plugin.getStoryManager().getPlayerCurrentNode(p);
            if (node == null || node.getObjectives().isEmpty())
                return "None";
            StoryNode.StoryObjective obj = node.getObjectives().get(0); // Show first objective
            return obj.getDescription();
        }

        return null;
    }
}
