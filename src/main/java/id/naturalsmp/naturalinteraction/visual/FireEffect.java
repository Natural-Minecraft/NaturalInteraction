package id.naturalsmp.naturalinteraction.visual;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Fire/Api effect: Spiral flames rising around NPC.
 * Particles: FLAME spiral, LAVA + SMOKE at ground, occasional small burst.
 */
public class FireEffect implements ElementalEffect {

    private final double spiralHeight;

    public FireEffect(double spiralHeight) {
        this.spiralHeight = spiralHeight;
    }

    @Override
    public void render(Location npcLocation, long tick) {
        World world = npcLocation.getWorld();
        if (world == null) return;

        double baseAngle = (tick * 0.2) % (2 * Math.PI);

        // Double helix FLAME spiral rising around NPC
        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI; // 180° apart
            for (int i = 0; i < 8; i++) {
                double progress = i / 8.0; // 0.0 to 1.0
                double a = baseAngle + strandOffset + progress * 2.0 * Math.PI;
                double radius = 0.8 + progress * 0.3; // widening spiral
                double px = npcLocation.getX() + Math.cos(a) * radius;
                double py = npcLocation.getY() + progress * spiralHeight;
                double pz = npcLocation.getZ() + Math.sin(a) * radius;
                world.spawnParticle(Particle.FLAME, px, py, pz, 0, 0, 0.02, 0, 0.01);
            }
        }

        // Ground fire ring (LAVA + SMOKE_NORMAL)
        if (tick % 3 == 0) {
            int points = 10;
            for (int i = 0; i < points; i++) {
                double a = (2 * Math.PI * i / points) + baseAngle * 0.3;
                double r = 1.0;
                double px = npcLocation.getX() + Math.cos(a) * r;
                double pz = npcLocation.getZ() + Math.sin(a) * r;
                world.spawnParticle(Particle.LAVA,
                        new Location(world, px, npcLocation.getY() + 0.1, pz),
                        0, 0, 0, 0, 0);
            }
        }

        // Smoke rising at ground level
        if (tick % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * 1.5;
                double oz = (Math.random() - 0.5) * 1.5;
                world.spawnParticle(Particle.SMOKE,
                        npcLocation.getX() + ox,
                        npcLocation.getY() + 0.2,
                        npcLocation.getZ() + oz,
                        1, 0, 0.1, 0, 0.02);
            }
        }

        // Ember particles floating up (small flame)
        if (tick % 6 == 0) {
            for (int i = 0; i < 4; i++) {
                double ox = (Math.random() - 0.5) * 1.0;
                double oz = (Math.random() - 0.5) * 1.0;
                world.spawnParticle(Particle.SMALL_FLAME,
                        npcLocation.getX() + ox,
                        npcLocation.getY() + Math.random() * 2.0,
                        npcLocation.getZ() + oz,
                        0, 0, 0.03, 0, 0.01);
            }
        }

        // Occasional fire burst
        if (tick % 50 == 0) {
            world.spawnParticle(Particle.FLAME,
                    npcLocation.clone().add(0, 1.0, 0),
                    20, 0.4, 0.5, 0.4, 0.05);
            world.spawnParticle(Particle.SMOKE,
                    npcLocation.clone().add(0, 1.5, 0),
                    10, 0.3, 0.3, 0.3, 0.03);
        }
    }
}
