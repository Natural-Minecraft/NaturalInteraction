package id.naturalsmp.naturalinteraction.session;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Controls the BossBar countdown timer for a single dialogue node.
 * On timeout, invokes the provided Runnable on the main thread.
 */
public class TimerController {

    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;

    private BossBar bossBar;
    private BukkitRunnable timerTask;

    public TimerController(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;
    }

    /**
     * Start (or restart) the countdown for the given node.
     * {@code onTimeout} is called on the main thread when time expires.
     */
    public void start(DialogueNode node, Runnable onTimeout) {
        stop(); // Cancel any existing timer first

        String displayName = buildDisplayName();
        bossBar = BossBar.bossBar(
                Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(displayName, NamedTextColor.YELLOW))
                        .append(Component.text(" ✦", NamedTextColor.GOLD)),
                1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);

        final int durationTicks = node.getDurationSeconds() * 20;

        timerTask = new BukkitRunnable() {
            int ticksLeft = durationTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    plugin.getInteractionManager().endInteraction(player.getUniqueId());
                    return;
                }
                ticksLeft -= 2;
                bossBar.progress(Math.max(0f,
                        durationTicks > 0 ? (float) ticksLeft / durationTicks : 0f));
                if (ticksLeft <= 0) {
                    cancel();
                    onTimeout.run();
                }
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 2L);
    }

    /** Stop and cancel the current timer task. */
    public void stop() {
        if (timerTask != null && !timerTask.isCancelled()) timerTask.cancel();
    }

    /** Hide the BossBar from the player. */
    public void hide() {
        if (bossBar != null && player.isOnline()) player.hideBossBar(bossBar);
    }

    public void setProgress(float progress) {
        if (bossBar != null) bossBar.progress(Math.max(0f, Math.min(1f, progress)));
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private String buildDisplayName() {
        String raw = interaction.getId().replace("_", " ");
        return raw.isEmpty() ? raw : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
