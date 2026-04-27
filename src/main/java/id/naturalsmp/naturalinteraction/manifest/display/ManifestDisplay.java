package id.naturalsmp.naturalinteraction.manifest.display;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.entity.Player;

/**
 * A display that can be shown/hidden to a player.
 * Used by AudienceDisplay manifest entries.
 */
public interface ManifestDisplay {

    String getType();

    /** Show the display content to the player. */
    void show(Player player, NaturalInteraction plugin);

    /** Hide/remove the display content from the player. */
    void hide(Player player, NaturalInteraction plugin);
}
