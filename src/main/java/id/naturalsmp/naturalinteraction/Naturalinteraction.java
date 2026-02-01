package id.naturalsmp.naturalinteraction;

import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.commands.InteractionCommand;
import id.naturalsmp.naturalinteraction.interaction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.interaction.hook.CitizensHook;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalInteraction extends JavaPlugin {

    private static NaturalInteraction instance;
    private StoryManager storyManager;
    private StoryNPCManager npcManager;
    private InteractionManager interactionManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();

        // Managers
        this.storyManager = new StoryManager(this);
        this.npcManager = new StoryNPCManager(this);
        this.interactionManager = new InteractionManager(this);

        // Citizens
        CitizensHook.registerTraits(this);

        // PAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new id.naturalsmp.naturalinteraction.utils.NaturalInteractionExpansion(this).register();
        }

        // Listeners
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.story.StoryListener(storyManager), this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.utils.EditorListener(this), this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.interaction.listener.InteractionListener(this),
                        this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.interaction.gui.GUIListener(), this);

        // Commands
        getCommand("interaction").setExecutor(new InteractionCommand(this));

        getLogger().info(
                ChatUtils.colorize("<gradient:#4facfe:#00f2fe>NaturalInteraction</gradient> <white>has been enabled!"));
    }

    @Override
    public void onDisable() {
        if (storyManager != null) {
            storyManager.saveProgress();
        }
    }

    public static NaturalInteraction getInstance() {
        return instance;
    }

    public StoryManager getStoryManager() {
        return storyManager;
    }

    public StoryNPCManager getNpcManager() {
        return npcManager;
    }

    public InteractionManager getInteractionManager() {
        return interactionManager;
    }
}