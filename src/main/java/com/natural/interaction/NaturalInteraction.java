package com.natural.interaction;

import com.natural.interaction.manager.InteractionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class NaturalInteraction extends JavaPlugin {
    
    private static NaturalInteraction instance;
    private InteractionManager interactionManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize Managers
        this.interactionManager = new InteractionManager(this);
        
        // Register Citizens Implementation
        com.natural.interaction.hook.CitizensHook.registerTraits(this);
        
        // Register Commands
        getCommand("interaction").setExecutor(new com.natural.interaction.command.InteractionCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new com.natural.interaction.listener.InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new com.natural.interaction.gui.GUIListener(), this);
        
        getLogger().info("NaturalInteraction has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save data
        getLogger().info("NaturalInteraction has been disabled!");
    }

    public static NaturalInteraction getInstance() {
        return instance;
    }

    public InteractionManager getInteractionManager() {
        return interactionManager;
    }
}
