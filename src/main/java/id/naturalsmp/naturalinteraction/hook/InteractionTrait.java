package id.naturalsmp.naturalinteraction.hook;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

@TraitName("interaction")
public class InteractionTrait extends Trait {

    @Persist
    private String interactionId = null;

    public InteractionTrait() {
        super("interaction");
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getInteractionId() {
        return interactionId;
    }
}
