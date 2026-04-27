package id.naturalsmp.naturalinteraction;

import id.naturalsmp.naturalinteraction.commands.InteractionCommand;
import id.naturalsmp.naturalinteraction.commands.NiCommand;
import id.naturalsmp.naturalinteraction.commands.SidequestCommand;
import id.naturalsmp.naturalinteraction.commands.StoryCommand;
import id.naturalsmp.naturalinteraction.editor.EditorMode;
import id.naturalsmp.naturalinteraction.facts.FactsManager;
import id.naturalsmp.naturalinteraction.hook.CitizensHook;
import id.naturalsmp.naturalinteraction.listener.DungeonCompletionListener;
import id.naturalsmp.naturalinteraction.listener.EditorListener;
import id.naturalsmp.naturalinteraction.listener.InteractionListener;
import id.naturalsmp.naturalinteraction.listener.PrologueJoinListener;
import id.naturalsmp.naturalinteraction.listener.ScrollListener;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.npc.StoryNPCManager;
import id.naturalsmp.naturalinteraction.story.StoryListener;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.NaturalInteractionExpansion;
import id.naturalsmp.naturalinteraction.visual.ElementalEffectManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalInteraction extends JavaPlugin {

    private static NaturalInteraction instance;

    private StoryManager storyManager;
    private StoryNPCManager npcManager;
    private InteractionManager interactionManager;
    private EditorMode editorMode;
    private PrologueJoinListener prologueJoinListener;
    private ElementalEffectManager elementalEffectManager;
    private FactsManager factsManager;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();

        // Managers
        this.factsManager       = new FactsManager(this);
        this.storyManager       = new StoryManager(this);
        this.npcManager         = new StoryNPCManager(this);
        this.interactionManager = new InteractionManager(this);
        this.editorMode         = new EditorMode(this);

        // Citizens traits
        CitizensHook.registerTraits(this);

        // PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NaturalInteractionExpansion(this).register();
        }

        registerEvents();
        registerCommands();

        // Register ProtocolLib scroll listener for dialogue option cycling
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new ScrollListener(this));
            getLogger().info("ProtocolLib scroll listener registered.");
        } else {
            getLogger().warning("ProtocolLib not found — scroll-to-cycle will use hotbar fallback only.");
        }

        // Elemental NPC visual effects
        this.elementalEffectManager = new ElementalEffectManager(this);

        getLogger().info(
                ChatUtils.colorize("<gradient:#4facfe:#00f2fe>NaturalInteraction</gradient> <white>v"
                        + getDescription().getVersion() + " has been enabled!"));
    }

    @Override
    public void onDisable() {
        if (editorMode != null)         editorMode.disableAll();
        if (storyManager != null)       storyManager.saveProgress();
        if (elementalEffectManager != null) elementalEffectManager.stop();
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    private void registerEvents() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(new StoryListener(storyManager), this);
        pm.registerEvents(new EditorListener(this), this);           // Now in listener package
        pm.registerEvents(new InteractionListener(this), this);
        pm.registerEvents(new id.naturalsmp.naturalinteraction.gui.GUIListener(), this);
        pm.registerEvents(new id.naturalsmp.naturalinteraction.editor.EditorHotbarListener(this), this);

        // Prologue join enforcement
        this.prologueJoinListener = new PrologueJoinListener(this);
        pm.registerEvents(prologueJoinListener, this);

        // Dungeon-Story integration (soft dependency)
        if (getServer().getPluginManager().getPlugin("NaturalDungeon") != null) {
            pm.registerEvents(new DungeonCompletionListener(this), this);
            getLogger().info("Dungeon-Story Integration enabled.");
        }
    }

    private void registerCommands() {
        // /ni — main command (v2), aliases: /interaction, /inter
        NiCommand niCmd = new NiCommand(this);
        var niEntry = getCommand("ni");
        if (niEntry != null) {
            niEntry.setExecutor(niCmd);
            niEntry.setTabCompleter(niCmd);
        }

        if (getCommand("story") != null) {
            getCommand("story").setExecutor(new StoryCommand(this));
        }
        if (getCommand("sidequest") != null) {
            getCommand("sidequest").setExecutor(new SidequestCommand(this));
        }
    }

    // ─── Plugin Reload ────────────────────────────────────────────────────────

    public void reloadPlugin() {
        reloadConfig();
        if (storyManager != null)           storyManager.loadNodes();
        if (interactionManager != null)     interactionManager.loadInteractions();
        if (elementalEffectManager != null) elementalEffectManager.reload();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public static NaturalInteraction getInstance()              { return instance; }
    public StoryManager getStoryManager()                       { return storyManager; }
    public StoryNPCManager getNpcManager()                      { return npcManager; }
    public InteractionManager getInteractionManager()           { return interactionManager; }
    public EditorMode getEditorMode()                           { return editorMode; }
    public PrologueJoinListener getPrologueJoinListener()       { return prologueJoinListener; }
    public ElementalEffectManager getElementalEffectManager()   { return elementalEffectManager; }
    public FactsManager getFactsManager()                       { return factsManager; }
}
