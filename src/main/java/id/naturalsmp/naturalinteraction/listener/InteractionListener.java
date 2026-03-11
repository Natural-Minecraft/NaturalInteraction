package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import id.naturalsmp.naturalinteraction.manager.InteractionSession;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class InteractionListener implements Listener {

    private final NaturalInteraction plugin;

    public InteractionListener(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCClick(NPCRightClickEvent event) {
        if (event.getNPC().hasTrait(InteractionTrait.class)) {
            InteractionTrait trait = event.getNPC().getTrait(InteractionTrait.class);
            String interactionId = trait.getInteractionId();

            if (interactionId != null) {
                plugin.getInteractionManager().startInteraction(event.getClicker(), interactionId);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDealtDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Block movement during interaction (cinematic lock enforcement)
     */
    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session != null) {
            // Allow looking around (head rotation) but block position change
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Sneak to Skip — press shift to skip dialogue / advance
     */
    @EventHandler
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return; // Only trigger on sneak START

        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session != null) {
            session.skip();
        }
    }

    /**
     * Click floating TextDisplay choices above NPC head
     */
    @EventHandler
    public void onChoiceClick(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.Interaction interactionEntity) {
            org.bukkit.persistence.PersistentDataContainer pdc = interactionEntity.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "choice_index");

            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int index = pdc.get(key, org.bukkit.persistence.PersistentDataType.INTEGER);
                Player player = event.getPlayer();
                InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());

                if (session != null) {
                    session.selectOptionByIndex(index);
                }
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        InteractionSession session = plugin.getInteractionManager().getSession(player.getUniqueId());
        if (session != null && session.isDisplayingOptions()) {
            event.setCancelled(true);
            int slot = event.getNewSlot(); // 0 to 8
            session.selectOptionByIndex(slot);
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (plugin.getInteractionManager().getSession(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("naturalsmp.admin"))
            return;

        // Any command blocked during interaction
        if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cSelesaikan percakapan terlebih dahulu!"));
            return;
        }

        // Block ALL commands for players who haven't completed prologue
        if (!plugin.getInteractionManager().getCompletionTracker()
                .hasCompleted(player.getUniqueId(), "prologue")) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cKamu harus menyelesaikan prologue terlebih dahulu!"));
            return;
        }

        // Specific block for quest_sky world
        if (player.getWorld().getName().equalsIgnoreCase("quest_sky")) {
            event.setCancelled(true);
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("&cKamu belum bisa menggunakan command di dunia ini. Selesaikan prologue-nya!"));
        }
    }
}
