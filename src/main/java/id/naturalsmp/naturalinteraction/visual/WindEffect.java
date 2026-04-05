package id.naturalsmp.naturalinteraction.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Wind/Angin effect: NPC floats 3 blocks up, bobs ±1 block sinusoidal.
 * Particles: CLOUD swirling around + SWEEP_ATTACK sparks.
 */
public class WindEffect implements ElementalEffect {

    private final double floatHeight;
    private final double bobAmplitude;
    private final double bobPeriodTicks; // period in ticks

    public WindEffect(double floatHeight, double bobAmplitude, double bobPeriodSeconds) {
        this.floatHeight = floatHeight;
        this.bobAmplitude = bobAmplitude;
        this.bobPeriodTicks = bobPeriodSeconds * 20.0; // convert to ticks
    }

    // Hard limits: NPC can never float more than MAX_FLOAT above base Y
    private static final double MAX_FLOAT_LIMIT = 5.0;

    @Override
    public void render(Location npcLocation, long tick) {
        World world = npcLocation.getWorld();
        if (world == null) return;

        double angle = (tick * 0.15) % (2 * Math.PI);

        // Swirling CLOUD around NPC (radius ~1.2)
        double radius = 1.2;
        for (int i = 0; i < 3; i++) {
            double a = angle + (i * 2 * Math.PI / 3);
            double px = npcLocation.getX() + Math.cos(a) * radius;
            double py = npcLocation.getY() + 1.0;
            double pz = npcLocation.getZ() + Math.sin(a) * radius;
            world.spawnParticle(Particle.CLOUD, px, py, pz, 0, 0, 0.05, 0, 0.02);
        }

        // Upward wind particles below NPC (connecting ground to floating NPC)
        if (tick % 4 == 0) {
            for (int i = 0; i < 5; i++) {
                double ox = (Math.random() - 0.5) * 1.5;
                double oz = (Math.random() - 0.5) * 1.5;
                double oy = Math.random() * floatHeight;
                world.spawnParticle(Particle.CLOUD, npcLocation.getX() + ox,
                        npcLocation.getY() - floatHeight + oy + bobAmplitude,
                        npcLocation.getZ() + oz, 0, 0, 0.1, 0, 0.01);
            }
        }

        // SWEEP_ATTACK sparkle (sparse)
        if (tick % 10 == 0) {
            world.spawnParticle(Particle.SWEEP_ATTACK,
                    npcLocation.getX() + (Math.random() - 0.5) * 2,
                    npcLocation.getY() + 0.5 + Math.random(),
                    npcLocation.getZ() + (Math.random() - 0.5) * 2,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    public Location getAdjustedNPCLocation(Location base, long tick) {
        // Guard: avoid division by zero / NaN if period misconfigured
        double safeOffset = 0.0;
        if (bobPeriodTicks > 0) {
            double raw = Math.sin(2 * Math.PI * tick / bobPeriodTicks) * bobAmplitude;
            // Guard NaN / Infinity
            if (Double.isFinite(raw)) {
                safeOffset = raw;
            }
        }

        // Clamp: total Y offset must stay within [floatHeight - bobAmplitude, floatHeight + bobAmplitude]
        // AND never exceed MAX_FLOAT_LIMIT above base
        double totalOffset = floatHeight + safeOffset;
        double minOffset = Math.max(0.0, floatHeight - bobAmplitude);
        double maxOffset = Math.min(MAX_FLOAT_LIMIT, floatHeight + bobAmplitude);
        totalOffset = Math.max(minOffset, Math.min(maxOffset, totalOffset));

        return base.clone().add(0, totalOffset, 0);
    }
}
