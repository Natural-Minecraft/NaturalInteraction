package id.naturalsmp.naturalinteraction.manifest.display;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.entity.Player;

/** Shows an ActionBar message while the player is in the audience. Re-sent each tick. */
public class ActionBarDisplay implements ManifestDisplay {
    private final String text;

    public ActionBarDisplay(String text) { this.text = text; }

    @Override public String getType() { return "action_bar"; }

    @Override
    public void show(Player player, NaturalInteraction plugin) {
        player.sendActionBar(ChatUtils.toComponent(text.replace("%player%", player.getName())));
    }

    @Override
    public void hide(Player player, NaturalInteraction plugin) {
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
    }
}
