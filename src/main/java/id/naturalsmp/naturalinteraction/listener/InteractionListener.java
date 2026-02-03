package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.hook.InteractionTrait;
import net.citizensnpcs.api.event.NPCRightClickEvent;
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
                event.setCancelled(true); // Prevent default Citizens interaction
            }
        }
    }

    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDealtDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player) {
            if (plugin.getInteractionManager().getSession(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChoiceClick(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof org.bukkit.entity.Interaction interactionEntity) {
            org.bukkit.persistence.PersistentDataContainer pdc = interactionEntity.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "choice_index");

            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                int index = pdc.get(key, org.bukkit.persistence.PersistentDataType.INTEGER);
                org.bukkit.entity.Player player = event.getPlayer();
                id.naturalsmp.naturalinteraction.manager.InteractionSession session = plugin.getInteractionManager()
                        .getSession(player.getUniqueId());

                if (session != null) {
                    id.naturalsmp.naturalinteraction.model.Interaction interaction = null;
                    // We need access to the interaction object or just trigger a method in session
                    // Fortunately, selectOption in session takes an Option.
                    // I'll add a selectOptionByIndex method to InteractionSession for convenience.
                    session.selectOptionByIndex(index);
                }
            }
        }
    }
}
