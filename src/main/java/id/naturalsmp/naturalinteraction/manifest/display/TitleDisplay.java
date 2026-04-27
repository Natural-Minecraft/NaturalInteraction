package id.naturalsmp.naturalinteraction.manifest.display;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

/** Shows a title+subtitle once when the player enters the audience. */
public class TitleDisplay implements ManifestDisplay {
    private final String title;
    private final String subtitle;

    public TitleDisplay(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle != null ? subtitle : "";
    }

    @Override public String getType() { return "title"; }

    @Override
    public void show(Player player, NaturalInteraction plugin) {
        player.showTitle(Title.title(
                ChatUtils.toComponent(title.replace("%player%", player.getName())),
                ChatUtils.toComponent(subtitle.replace("%player%", player.getName())),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
    }

    @Override
    public void hide(Player player, NaturalInteraction plugin) {
        player.clearTitle();
    }
}
