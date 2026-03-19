package id.naturalsmp.naturalinteraction.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Water/Air effect: Abstract water platform beneath NPC.
 * Particles: WATER_SPLASH ring, DRIPPING_WATER, bubble column.
 * Creates a visual 5x5 water-like surface beneath the NPC.
 */
public class WaterEffect implements ElementalEffect {

    private final double groundRadius;

    public WaterEffect(double groundRadius) {
        this.groundRadius = groundRadius;
    }

    @Override
    public void render(Location npcLocation, long tick) {
        World world = npcLocation.getWorld();
        if (world == null) return;

        double angle = (tick * 0.08) % (2 * Math.PI);

        // Outer ring of WATER_SPLASH
        if (tick % 2 == 0) {
            int points = 16;
            for (int i = 0; i < points; i++) {
                double a = (2 * Math.PI * i / points) + angle;
                double px = npcLocation.getX() + Math.cos(a) * groundRadius;
                double pz = npcLocation.getZ() + Math.sin(a) * groundRadius;
                world.spawnParticle(Particle.SPLASH,
                        new Location(world, px, npcLocation.getY() + 0.1, pz),
                        1, 0.05, 0.01, 0.05, 0.01);
            }
        }

        // Inner water surface effect (scattered splash particles within radius)
        if (tick % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                double rx = (Math.random() - 0.5) * groundRadius * 2;
                double rz = (Math.random() - 0.5) * groundRadius * 2;
                // Only within circular radius
                if (rx * rx + rz * rz <= groundRadius * groundRadius) {
                    world.spawnParticle(Particle.SPLASH,
                            npcLocation.getX() + rx,
                            npcLocation.getY() + 0.05,
                            npcLocation.getZ() + rz,
                            1, 0, 0, 0, 0.01);
                }
            }
        }

        // Dripping water around NPC body
        if (tick % 3 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = angle + (i * 2 * Math.PI / 3);
                double px = npcLocation.getX() + Math.cos(a) * 0.6;
                double py = npcLocation.getY() + 0.5 + Math.random() * 1.5;
                double pz = npcLocation.getZ() + Math.sin(a) * 0.6;
                world.spawnParticle(Particle.DRIPPING_WATER, px, py, pz, 1, 0.1, 0.1, 0.1, 0);
            }
        }

        // Bubble column effect rising from ground to NPC
        if (tick % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double ox = (Math.random() - 0.5) * 0.8;
                double oz = (Math.random() - 0.5) * 0.8;
                world.spawnParticle(Particle.BUBBLE_POP,
                        npcLocation.getX() + ox,
                        npcLocation.getY() + Math.random() * 2,
                        npcLocation.getZ() + oz,
                        1, 0, 0.05, 0, 0.01);
            }
        }

        // Occasional ripple burst
        if (tick % 30 == 0) {
            int ripplePoints = 20;
            for (int i = 0; i < ripplePoints; i++) {
                double a = 2 * Math.PI * i / ripplePoints;
                double r = groundRadius * 0.8;
                world.spawnParticle(Particle.SPLASH,
                        npcLocation.getX() + Math.cos(a) * r,
                        npcLocation.getY() + 0.15,
                        npcLocation.getZ() + Math.sin(a) * r,
                        3, 0, 0.05, 0, 0.02);
            }
        }
    }
}
