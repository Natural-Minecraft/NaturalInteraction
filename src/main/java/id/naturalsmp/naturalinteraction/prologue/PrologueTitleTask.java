package id.naturalsmp.naturalinteraction.prologue;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

/**
 * Phase 1 — Looping "Login Screen" title.
 * Shows custom font image character as title, no subtitle text (text is baked into the image).
 * Runs until cancelled (player crouches → PrologueCinematicHandler cancels this).
 */
public class PrologueTitleTask extends BukkitRunnable {

    private final Player player;
    private final String titleChar;

    public PrologueTitleTask(Player player, String titleChar) {
        this.player = player;
        this.titleChar = titleChar;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        // Show the image character as title with no subtitle.
        // stay=50 ticks (2.5s) with 0 fade-in / fade-out so it appears instant and loops cleanly.
        player.showTitle(Title.title(
                ChatUtils.toComponent(titleChar),
                Component.empty(),
                Title.Times.times(
                        Duration.ZERO,           // fade in
                        Duration.ofMillis(2500), // stay (must be >= task period)
                        Duration.ZERO            // fade out
                )
        ));
    }
}
