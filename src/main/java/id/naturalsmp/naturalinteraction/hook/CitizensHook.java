package id.naturalsmp.naturalinteraction.hook;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class CitizensHook {
    
    public static void registerTraits(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Citizens") == null) {
            plugin.getLogger().log(Level.SEVERE, "Citizens is not installed! Interaction features will not work.");
            return;
        }

        try {
            CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(InteractionTrait.class));
            plugin.getLogger().info("Registered InteractionTrait with Citizens.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register InteractionTrait", e);
        }
    }
}