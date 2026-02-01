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
                event.setCancelled(true); // Prevent default Citizens interaction (like trading if it was a villager?)
            }
        }
    }
}