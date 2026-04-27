package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.manager.InteractionSession;
import id.naturalsmp.naturalinteraction.utils.PluginConfig;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

/**
 * Handles all player input during an active InteractionSession.
 *
 * Input mapping (TypewriterMC-style):
 *  Right-click (no target)    → cycle to next option
 *  Left-click (Attack)        → confirm highlighted option
 *  "F" (swap hand)            → confirm highlighted option (alt)
 *  Scroll / Hotbar change     → handled by ScrollListener (ProtocolLib)
 *                               + PlayerItemHeldEvent fallback (direct slot jump)
 *  Shift                      → skip / advance dialogue
 *  NPC Right-click            → start interaction
 */
public class InteractionListener implements Listener {

    private final NaturalInteraction plugin;

    public InteractionListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    // ─── Start Interaction ────────────────────────────────────────────────────

    @EventHandler
    public void onNPCClick(NPCRightClickEvent event) {
        if (!event.getNPC().hasTrait(InteractionTrait.class)) return;
        InteractionTrait trait = event.getNPC().getTrait(InteractionTrait.class);
        String interactionId = trait.getInteractionId();
        if (interactionId != null) {
            plugin.getInteractionManager().startInteraction(event.getClicker(), interactionId);
            event.setCancelled(true);
        }
    }

    // ─── Input: Right-click → Cycle Next ─────────────────────────────────────

    /**
     * Right-click in the air (Place Block action, no target block) → cycle next option.
     * We must allow NPC right-click to pass through to NPCRightClickEvent above.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session == null) return;

        Action action = event.getAction();

        // Right-click with no block target → cycle next
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && session.isDisplayingOptions()) {
            event.setCancelled(true);
            session.cycleNext();
            return;
        }

        // Left-click → confirm selected option
        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
                && session.isDisplayingOptions()) {
            event.setCancelled(true);
            session.confirmSelected();
            return;
        }

        // Cancel all other interactions during a session
        event.setCancelled(true);
    }

    // ─── Input: "F" key (Swap Hand) → Confirm ────────────────────────────────

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        if (session.isDisplayingOptions()) {
            session.confirmSelected();
        }
    }

    // ─── Input: Hotbar Slot Change → Jump to Option (fallback for scroll) ────

    /**
     * PlayerItemHeldEvent fires on:
     *  1. Mouse scroll (primary method if ProtocolLib scroll fails)
     *  2. Number key press (1-9)
     *
     * We use slot index as direct option jump. Infinite scroll wraps.
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session == null || !session.isDisplayingOptions()) return;

        event.setCancelled(true); // Don't actually change the held slot

        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();

        // Detect scroll direction from slot delta
        // Scroll up: slot decreases (or wraps 0 → 8)
        // Scroll down: slot increases (or wraps 8 → 0)
        boolean scrolledUp = (next < prev && !(prev == 8 && next == 0))
                || (prev == 0 && next == 8);

        if (scrolledUp) {
            session.cyclePrev();
        } else {
            session.cycleNext();
        }
    }

    // ─── Input: Shift → Skip / Advance ───────────────────────────────────────

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // Only on sneak start
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session != null) {
            session.skip();
        }
    }

    // ─── Protective Cancels ───────────────────────────────────────────────────

    @EventHandler
    public void onDamageReceived(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    /** Block position change but allow head rotation (looking around). */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) == null) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getInteractionManager().getSession(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    // ─── Command Blocking ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("naturalsmp.admin")) return;

        // Block commands during active session
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cSelesaikan percakapan terlebih dahulu!"));
            return;
        }

        // Block commands for players who haven't completed prologue
        String prologueId = PluginConfig.getPrologueInteractionId(plugin);
        if (!plugin.getInteractionManager().getCompletionTracker()
                .hasCompleted(player.getUniqueId(), prologueId)) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cKamu harus menyelesaikan prologue terlebih dahulu!"));
            return;
        }

        // Block commands inside prologue world
        String prologueWorld = PluginConfig.getPrologueWorld(plugin);
        if (player.getWorld().getName().equalsIgnoreCase(prologueWorld)) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cKamu belum bisa menggunakan command di dunia ini!"));
        }
    }

    // ─── Disconnect Cleanup ───────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session != null) {
            session.forceCleanup();
        }
    }
}
