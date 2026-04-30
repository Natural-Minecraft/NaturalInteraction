package id.naturalsmp.naturalinteraction.cinematic;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
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

        ArmorStand mount = (ArmorStand) originalLocation.getWorld().spawnEntity(originalLocation, EntityType.ARMOR_STAND);
        mount.setVisible(false);
        mount.setGravity(false);
        mount.setMarker(true);
        mount.setInvulnerable(true);
        mount.setBasePlate(false);

        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setSpectatorTarget(mount);
        } else {
            mount.addPassenger(player);
        }

        ActiveCinematic active = new ActiveCinematic(sequence, originalLocation, originalMode, mount);
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
                
                CameraPoint prev = pointIndex - 1 >= 0
                        ? points.get(pointIndex - 1)
                        : (sequence.isLoop() ? points.get(points.size() - 1) : current);
                
                CameraPoint nextNext = pointIndex + 2 < points.size()
                        ? points.get(pointIndex + 2)
                        : (sequence.isLoop() ? points.get((pointIndex + 2) % points.size()) : next);

                // Display subtitle text at the start of the point
                if (ticksInCurrentPoint == 0 && current.getText() != null && !current.getText().isEmpty()) {
                    player.sendActionBar(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(current.getText()));
                }

                // Handle Cross-World: Instant teleport
                if (!current.getLocation().getWorld().equals(next.getLocation().getWorld())) {
                    if (ticksInCurrentPoint == 0) {
                        mount.teleport(current.getLocation().clone().add(0, 1.62, 0));
                        player.teleport(current.getLocation().clone().add(0, 1.62, 0));
                    }
                    if (ticksInCurrentPoint >= current.getDurationTicks() - 1) {
                        mount.teleport(next.getLocation().clone().add(0, 1.62, 0));
                        player.teleport(next.getLocation().clone().add(0, 1.62, 0));
                        ticksInCurrentPoint = current.getDurationTicks(); // Force advance
                    } else {
                        ticksInCurrentPoint++;
                        tick++;
                        return;
                    }
                } else {
                    // Progress within this point (0.0 → 1.0)
                    float progress = current.getDurationTicks() > 0
                            ? (float) ticksInCurrentPoint / current.getDurationTicks() : 1.0f;

                    // Apply easing
                    float easedProgress = applyEasing(progress, current.getEasing());

                    // Interpolate location and offset by 1.62 (player eye height)
                    // because Marker ArmorStands have an eye height of 0.
                    Location interpolated = interpolate(
                            prev.getLocation(), current.getLocation(), next.getLocation(), nextNext.getLocation(),
                            current.getYaw(), current.getPitch(),
                            next.getYaw(), next.getPitch(),
                            easedProgress);

                    Location mountLoc = interpolated.clone().add(0, 1.62, 0);
                    mount.teleport(mountLoc);

                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        // If not in spectator, they are a passenger. We need to sync rotation without dismounting
                        // Unfortunately, Bukkit teleport dismounts. The safest way is to just teleport the player directly
                        // and abandon the mount if they are not in spectator.
                        player.teleport(mountLoc);
                    }

                    ticksInCurrentPoint++;
                    tick++;
                }

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
        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null && player.getSpectatorTarget().equals(active.mount)) {
            player.setSpectatorTarget(null);
        }
        if (active.mount != null && active.mount.isValid()) {
            active.mount.removePassenger(player);
            active.mount.remove();
        }
        if (player.isOnline()) {
            player.setGameMode(active.originalMode);
            // Do NOT restore original location - leave player at the cinematic's end point
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private Location interpolate(Location p0, Location p1, Location p2, Location p3,
                                  float fromYaw, float fromPitch, float toYaw, float toPitch, float t) {
        // Catmull-Rom spline for position
        double t2 = t * t;
        double t3 = t2 * t;

        double f0 = -0.5 * t3 + t2 - 0.5 * t;
        double f1 = 1.5 * t3 - 2.5 * t2 + 1.0;
        double f2 = -1.5 * t3 + 2.0 * t2 + 0.5 * t;
        double f3 = 0.5 * t3 - 0.5 * t2;

        double x = p0.getX() * f0 + p1.getX() * f1 + p2.getX() * f2 + p3.getX() * f3;
        double y = p0.getY() * f0 + p1.getY() * f1 + p2.getY() * f2 + p3.getY() * f3;
        double z = p0.getZ() * f0 + p1.getZ() * f1 + p2.getZ() * f2 + p3.getZ() * f3;
        
        // Linear interpolation for rotation with wrap
        float diffYaw = toYaw - fromYaw;
        while (diffYaw < -180) diffYaw += 360;
        while (diffYaw > 180) diffYaw -= 360;
        float yaw = fromYaw + diffYaw * t;
        
        float pitch = fromPitch + (toPitch - fromPitch) * t;
        return new Location(p1.getWorld(), x, y, z, yaw, pitch);
    }

    private float applyEasing(float t, CameraPoint.EasingType easing) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> {
                if (t < 0.3333333f) yield 2.25f * t * t;
                if (t < 0.6666667f) yield 1.5f * t - 0.25f;
                float inv = 1 - t;
                yield 1 - 2.25f * inv * inv;
            }
            case SMOOTH -> (float) (3 * t * t - 2 * t * t * t); // Smoothstep
            case INSTANT -> 1.0f;
        };
    }

    public void reattach(Player player) {
        ActiveCinematic active = activeCinematics.get(player.getUniqueId());
        if (active != null && active.mount != null && player.getGameMode() == GameMode.SPECTATOR) {
            player.setSpectatorTarget(active.mount);
        }
    }

    // ─── Inner ────────────────────────────────────────────────────────────────

    private static class ActiveCinematic {
        final CinematicSequence sequence;
        final Location originalLocation;
        final GameMode originalMode;
        final ArmorStand mount;
        BukkitRunnable task;

        ActiveCinematic(CinematicSequence seq, Location loc, GameMode mode, ArmorStand mount) {
            this.sequence = seq;
            this.originalLocation = loc;
            this.originalMode = mode;
            this.mount = mount;
        }

        void setTask(BukkitRunnable task) { this.task = task; }
    }
}
