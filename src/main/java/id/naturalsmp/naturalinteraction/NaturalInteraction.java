package id.naturalsmp.naturalinteraction;

import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.commands.InteractionCommand;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.hook.CitizensHook;
import id.naturalsmp.naturalinteraction.npc.StoryNPCManager;
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

        registerEvents();

        // Commands
        InteractionCommand cmd = new InteractionCommand(this);
        getCommand("interaction").setExecutor(cmd);
        getCommand("interaction").setTabCompleter(cmd);

        getLogger().info(
                ChatUtils.colorize("<gradient:#4facfe:#00f2fe>NaturalInteraction</gradient> <white>has been enabled!"));
    }

    private void registerEvents() {
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.story.StoryListener(storyManager), this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.utils.EditorListener(this), this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.listener.InteractionListener(this),
                        this);
        getServer().getPluginManager()
                .registerEvents(new id.naturalsmp.naturalinteraction.gui.GUIListener(), this);
    }

    public void reloadPlugin() {
        reloadConfig();
        if (storyManager != null) {
            storyManager.loadNodes();
        }
        if (interactionManager != null) {
            interactionManager.loadInteractions();
        }
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
