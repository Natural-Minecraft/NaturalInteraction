package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.story.StoryNode;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for NaturalInteraction.
 *
 * Available placeholders:
 *  %naturalinteraction_current_title%            → current story title
 *  %naturalinteraction_current_objective%        → current story objective
 *  %naturalinteraction_in_session%               → true/false if player in dialogue
 *  %naturalinteraction_fact_<key>%               → value of any fact key
 *  %naturalinteraction_completed_<interaction>%  → true/false completion
 *  %naturalinteraction_tag_<name>%               → true/false for legacy tag
 */
public class NaturalInteractionExpansion extends PlaceholderExpansion {

    private final NaturalInteraction plugin;

    public NaturalInteractionExpansion(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "naturalinteraction"; }
    @Override public @NotNull String getAuthor()     { return "NaturalSMP"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        Player p = player.getPlayer();
        boolean online = p != null && p.isOnline();

        // %naturalinteraction_current_title%
        if (params.equalsIgnoreCase("current_title")) {
            if (!online) return "Offline";
            StoryNode node = plugin.getStoryManager().getPlayerCurrentNode(p);
            return node != null ? ChatUtils.serialize(ChatUtils.toComponent(node.getTitle())) : "No Quest";
        }

        // %naturalinteraction_current_objective%
        if (params.equalsIgnoreCase("current_objective")) {
            if (!online) return "Offline";
            StoryNode node = plugin.getStoryManager().getPlayerCurrentNode(p);
            if (node == null || node.getObjectives().isEmpty()) return "None";
            return node.getObjectives().get(0).getDescription();
        }

        // %naturalinteraction_in_session%
        if (params.equalsIgnoreCase("in_session")) {
            if (!online) return "false";
            return String.valueOf(plugin.getInteractionManager().getSession(p.getUniqueId()) != null);
        }

        // %naturalinteraction_fact_<key>%
        if (params.startsWith("fact_")) {
            String key = params.substring(5);
            return plugin.getFactsManager().getString(player.getUniqueId(), key, "");
        }

        // %naturalinteraction_completed_<interactionId>%
        if (params.startsWith("completed_")) {
            String id = params.substring(10);
            return String.valueOf(plugin.getFactsManager().hasCompleted(player.getUniqueId(), id));
        }

        // %naturalinteraction_tag_<tagName>%
        if (params.startsWith("tag_")) {
            String tag = params.substring(4);
            return String.valueOf(plugin.getFactsManager().hasTag(player.getUniqueId(), tag));
        }

        return null;
    }
}
