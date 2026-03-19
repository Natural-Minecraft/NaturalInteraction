package id.naturalsmp.naturalinteraction.visual;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Earth/Tanah effect: Ground-based rocky particles.
 * Particles: BLOCK_CRACK (stone) rotating at feet, ground circle,
 * occasional earth burst.
 */
public class EarthEffect implements ElementalEffect {

    private final double groundRadius;

    public EarthEffect(double groundRadius) {
        this.groundRadius = groundRadius;
    }

    @Override
    public void render(Location npcLocation, long tick) {
        World world = npcLocation.getWorld();
        if (world == null) return;

        double angle = (tick * 0.1) % (2 * Math.PI);

        // Rotating BLOCK_CRACK stones at feet (2 orbiting points)
        for (int i = 0; i < 2; i++) {
            double a = angle + (i * Math.PI);
            double px = npcLocation.getX() + Math.cos(a) * 0.8;
            double pz = npcLocation.getZ() + Math.sin(a) * 0.8;
            world.spawnParticle(Particle.BLOCK, new Location(world, px, npcLocation.getY() + 0.1, pz),
                    2, 0.1, 0.05, 0.1, 0.01,
                    Material.STONE.createBlockData());
        }

        // Ground circle of cracked stone particles (radius = groundRadius)
        if (tick % 3 == 0) {
            int points = 12;
            for (int i = 0; i < points; i++) {
                double a = (2 * Math.PI * i / points) + (tick * 0.02);
                double px = npcLocation.getX() + Math.cos(a) * groundRadius;
                double pz = npcLocation.getZ() + Math.sin(a) * groundRadius;
                world.spawnParticle(Particle.BLOCK,
                        new Location(world, px, npcLocation.getY() + 0.05, pz),
                        1, 0.05, 0, 0.05, 0,
                        Material.CRACKED_STONE_BRICKS.createBlockData());
            }
        }

        // Inner ground crack pattern
        if (tick % 5 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = (2 * Math.PI * i / 6) + (tick * 0.01);
                double r = groundRadius * 0.6;
                double px = npcLocation.getX() + Math.cos(a) * r;
                double pz = npcLocation.getZ() + Math.sin(a) * r;
                world.spawnParticle(Particle.BLOCK,
                        new Location(world, px, npcLocation.getY() + 0.02, pz),
                        1, 0.1, 0, 0.1, 0,
                        Material.COARSE_DIRT.createBlockData());
            }
        }

        // Occasional earth burst
        if (tick % 40 == 0) {
            world.spawnParticle(Particle.BLOCK,
                    npcLocation.clone().add(0, 0.5, 0),
                    15, 0.5, 0.3, 0.5, 0.05,
                    Material.DIRT.createBlockData());
        }
    }
}
