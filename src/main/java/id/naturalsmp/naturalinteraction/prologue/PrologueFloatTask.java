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

    private int tick = 0;
    private final Random random = new Random();

    public PrologueFloatTask(NaturalInteraction plugin, Player player, double speed, int totalTicks, Particle particle, Runnable onComplete) {
        this.plugin = plugin;
        this.player = player;
        this.speed = speed;
        this.totalTicks = totalTicks;
        this.particle = particle;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        tick++;

        // Only move the player downwards manually if they are NOT in a cinematic
        // If they are in a cinematic, the cinematic sequence itself controls their position.
        Location loc = player.getLocation();
        if (!plugin.getCinematicManager().getPlayer().isPlaying(player.getUniqueId())) {
            loc.subtract(0, speed, 0);
            player.teleport(loc);
            // Zero out velocity so physics don't interfere
            player.setVelocity(new Vector(0, 0, 0));
        }

        // Spawn particles every 3 ticks in a sphere around the player
        if (tick % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double rx = (random.nextDouble() - 0.5) * 2.0;
                double ry = random.nextDouble() * 2.0;
                double rz = (random.nextDouble() - 0.5) * 2.0;
                Location pLoc = loc.clone().add(rx, ry, rz);
                player.getWorld().spawnParticle(particle, pLoc, 1, 0, 0, 0, 0);
            }
        }

        // Also spawn HEART particles occasionally
        if (tick % 10 == 0) {
            for (int i = 0; i < 3; i++) {
                double rx = (random.nextDouble() - 0.5) * 1.5;
                double ry = random.nextDouble() * 2.5;
                double rz = (random.nextDouble() - 0.5) * 1.5;
                player.getWorld().spawnParticle(Particle.HEART, loc.clone().add(rx, ry, rz), 1, 0, 0, 0, 0);
            }
        }

        // When done, trigger Phase 3
        if (tick >= totalTicks) {
            cancel();
            onComplete.run();
        }
    }
}
