package id.naturalsmp.naturalinteraction.session;

import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.model.Interaction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

/**
 * Handles all cinematic movement-lock and NPC-facing logic for a dialogue session.
 * Extracted from InteractionSession to separate the "presentation" concern.
 */
public class CinematicController {

    private final Player player;
    private final Interaction interaction;

    public CinematicController(Player player, Interaction interaction) {
        this.player = player;
        this.interaction = interaction;
    }

    /** Apply slowness + jump-suppress potions (cinematic lock). */
    public void applyLock() {
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false, false));
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false, false));
    }

    /** Remove cinematic lock potions. */
    public void removeLock() {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    /**
     * Teleport the player 2.5 blocks in front of the interaction NPC, facing its head.
     * No-op if no matching NPC is found nearby.
     */
    public void faceNPC() {
        Location npcLoc = findNPCLocation();
        if (npcLoc == null) return;

        Location playerLoc = player.getLocation();
        Vector dir = playerLoc.toVector().subtract(npcLoc.toVector()).normalize();
        if (dir.lengthSquared() == 0 || Double.isNaN(dir.getX())) {
            dir = npcLoc.getDirection().multiply(-1);
        }

        Location target = npcLoc.clone().add(dir.multiply(2.5));
        target.setY(playerLoc.getY());
        target.setDirection(npcLoc.clone().add(0, 1.5, 0).toVector()
                .subtract(target.toVector()).normalize());

        player.teleport(target, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Find the stored location of the Citizens NPC associated with this interaction.
     * Returns null if Citizens is unavailable or no matching NPC is nearby.
     */
    @Nullable
    public Location findNPCLocation() {
        try {
            for (net.citizensnpcs.api.npc.NPC npc : net.citizensnpcs.api.CitizensAPI.getNPCRegistry()) {
                if (!npc.isSpawned()) continue;
                Location stored = npc.getStoredLocation();
                if (stored == null || !stored.getWorld().equals(player.getWorld())) continue;
                if (stored.distanceSquared(player.getLocation()) >= 25) continue;
                if (!npc.hasTrait(InteractionTrait.class)) continue;

                String npcInteractionId = npc.getTrait(InteractionTrait.class).getInteractionId();
                if (interaction.getId().equals(npcInteractionId)) return stored;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
