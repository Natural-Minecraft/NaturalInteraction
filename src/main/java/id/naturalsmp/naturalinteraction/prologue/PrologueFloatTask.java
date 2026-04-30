package id.naturalsmp.naturalinteraction.prologue;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Phase 2 — Float the player slowly downward while spawning healing-aura particles.
 * Called after the player crouches on the title screen.
 * When finished (reached target Y or ticks elapsed) → calls onComplete.
 */
public class PrologueFloatTask extends BukkitRunnable {

    private final Player player;
    private final double speed;      // blocks per tick
    private final int totalTicks;    // how many ticks to float
    private final Particle particle;
    private final Runnable onComplete;
    private final NaturalInteraction plugin;
    private final Location currentLoc;

    private int tick = 0;
    private final Random random = new Random();

    public PrologueFloatTask(NaturalInteraction plugin, Player player, double speed, int totalTicks, Particle particle, Runnable onComplete) {
        this.plugin = plugin;
        this.player = player;
        this.speed = speed;
        this.totalTicks = totalTicks;
        this.particle = particle;
        this.onComplete = onComplete;
        this.currentLoc = player.getLocation().clone();
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        tick++;

        // Track a floating location instead of teleporting the player directly
        // This makes particles float down gracefully in the center of the world
        // even if the player's camera (Spectator) is flying around in a cinematic.
        currentLoc.subtract(0, speed, 0);

        // Spawn particles every 3 ticks in a sphere around the floating location
        if (tick % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double rx = (random.nextDouble() - 0.5) * 2.0;
                double ry = random.nextDouble() * 2.0;
                double rz = (random.nextDouble() - 0.5) * 2.0;
                Location pLoc = currentLoc.clone().add(rx, ry, rz);
                player.getWorld().spawnParticle(particle, pLoc, 1, 0, 0, 0, 0);
            }
        }

        // Also spawn HEART particles occasionally
        if (tick % 10 == 0) {
            for (int i = 0; i < 3; i++) {
                double rx = (random.nextDouble() - 0.5) * 1.5;
                double ry = random.nextDouble() * 2.5;
                double rz = (random.nextDouble() - 0.5) * 1.5;
                player.getWorld().spawnParticle(Particle.HEART, currentLoc.clone().add(rx, ry, rz), 1, 0, 0, 0, 0);
            }
        }

        // When done, trigger Phase 3
        if (tick >= totalTicks) {
            cancel();
            // Teleport player to the final floating location so they don't fall
            player.teleport(currentLoc);
            player.setVelocity(new Vector(0, 0, 0));
            onComplete.run();
        }
    }
}
