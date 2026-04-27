package id.naturalsmp.naturalinteraction.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.InteractionSession;
import org.bukkit.entity.Player;

/**
 * Intercepts mouse scroll packets via ProtocolLib to cycle through dialogue options.
 *
 * Packet used: STEER_VEHICLE (0x1C in 1.21)
 * Fields: [forward (float), sideways (float), jump (bool), unmount (bool)]
 *
 * Scroll up   → forward > 0 → cyclePrev()
 * Scroll down → forward < 0 → cycleNext()
 *
 * Note: This only intercepts scrolling when the player is NOT mounted.
 * For mounted players, we use PlayerItemHeldEvent as fallback.
 */
public class ScrollListener extends PacketAdapter {

    private final NaturalInteraction plugin;

    public ScrollListener(NaturalInteraction plugin) {
        super(plugin, ListenerPriority.HIGH, PacketType.Play.Client.STEER_VEHICLE);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session == null || !session.isDisplayingOptions()) return;

        // STEER_VEHICLE: field 0 = sideways (strafe), field 1 = forward, field 2 = unmount, field 3 = jump
        // Scroll maps to the forward field (positive = up, negative = down)
        float forward = event.getPacket().getFloat().readSafely(1);

        if (forward > 0) {
            session.cyclePrev(); // Scroll up = go to previous option
        } else if (forward < 0) {
            session.cycleNext(); // Scroll down = go to next option
        } else {
            return; // No scroll, just movement — don't cancel
        }

        // Cancel the packet so the player doesn't actually move/steer
        event.setCancelled(true);
    }
}
