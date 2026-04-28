package id.naturalsmp.naturalinteraction.cinematic;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays cinematic sequences for players.
 * Uses spectator-like camera teleportation with smooth interpolation.
 */
public class CinematicPlayer {

    private final NaturalInteraction plugin;
    private final Map<UUID, ActiveCinematic> activeCinematics = new ConcurrentHashMap<>();

    public CinematicPlayer(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    /**
     * Start playing a cinematic for a player.
     */
    public void play(Player player, CinematicSequence sequence) {
        stop(player); // Stop any existing

        List<CameraPoint> points = sequence.getPoints();
        if (points.isEmpty()) return;

        // Save state
        Location originalLocation = player.getLocation().clone();
        GameMode originalMode = player.getGameMode();

        // Lock player
        if (sequence.isLockPlayerMovement()) {
            player.setGameMode(GameMode.SPECTATOR);
        }
        if (sequence.isHideHUD()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, 10, 0, false, false, false));
        }

        ActiveCinematic active = new ActiveCinematic(sequence, originalLocation, originalMode);
        activeCinematics.put(player.getUniqueId(), active);

        // Start interpolation task
        BukkitRunnable task = new BukkitRunnable() {
            int tick = 0;
            int pointIndex = 0;
            int ticksInCurrentPoint = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !activeCinematics.containsKey(player.getUniqueId())) {
                    cancel();
                    restore(player, active);
                    return;
                }

                CameraPoint current = points.get(pointIndex);
                CameraPoint next = pointIndex + 1 < points.size()
                        ? points.get(pointIndex + 1)
                        : (sequence.isLoop() ? points.get(0) : current);

                // Progress within this point (0.0 → 1.0)
                float progress = current.getDurationTicks() > 0
                        ? (float) ticksInCurrentPoint / current.getDurationTicks() : 1.0f;

                // Apply easing
                float easedProgress = applyEasing(progress, current.getEasing());

                // Interpolate location
                Location interpolated = interpolate(
                        current.getLocation(), current.getYaw(), current.getPitch(),
                        next.getLocation(), next.getYaw(), next.getPitch(),
                        easedProgress);

                player.teleport(interpolated);

                ticksInCurrentPoint++;
                tick++;

                if (ticksInCurrentPoint >= current.getDurationTicks()) {
                    // Move to next point
                    ticksInCurrentPoint = 0;
                    pointIndex++;

                    if (pointIndex >= points.size()) {
                        if (sequence.isLoop()) {
                            pointIndex = 0;
                        } else {
                            cancel();
                            restore(player, active);
                            return;
                        }
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 1L);
        active.setTask(task);
    }

    /**
     * Stop cinematic for a player.
     */
    public void stop(Player player) {
        ActiveCinematic active = activeCinematics.remove(player.getUniqueId());
        if (active != null) {
            if (active.task != null && !active.task.isCancelled()) active.task.cancel();
            restore(player, active);
        }
    }

    public boolean isPlaying(UUID player) {
        return activeCinematics.containsKey(player);
    }

    public void cleanup() {
        activeCinematics.forEach((uuid, active) -> {
            if (active.task != null && !active.task.isCancelled()) active.task.cancel();
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) restore(p, active);
        });
        activeCinematics.clear();
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private void restore(Player player, ActiveCinematic active) {
        activeCinematics.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.setGameMode(active.originalMode);
            player.teleport(active.originalLocation);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private Location interpolate(Location from, float fromYaw, float fromPitch,
                                  Location to, float toYaw, float toPitch, float t) {
        double x = from.getX() + (to.getX() - from.getX()) * t;
        double y = from.getY() + (to.getY() - from.getY()) * t;
        double z = from.getZ() + (to.getZ() - from.getZ()) * t;
        float yaw = fromYaw + (toYaw - fromYaw) * t;
        float pitch = fromPitch + (toPitch - fromPitch) * t;
        return new Location(from.getWorld(), x, y, z, yaw, pitch);
    }

    private float applyEasing(float t, CameraPoint.EasingType easing) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case SMOOTH -> (float) (3 * t * t - 2 * t * t * t); // Smoothstep
            case INSTANT -> 1.0f;
        };
    }

    // ─── Inner ────────────────────────────────────────────────────────────────

    private static class ActiveCinematic {
        final CinematicSequence sequence;
        final Location originalLocation;
        final GameMode originalMode;
        BukkitRunnable task;

        ActiveCinematic(CinematicSequence seq, Location loc, GameMode mode) {
            this.sequence = seq;
            this.originalLocation = loc;
            this.originalMode = mode;
        }

        void setTask(BukkitRunnable task) { this.task = task; }
    }
}
