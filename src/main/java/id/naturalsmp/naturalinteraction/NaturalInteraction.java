package id.naturalsmp.naturalinteraction;

import id.naturalsmp.naturalinteraction.commands.NiCommand;
import id.naturalsmp.naturalinteraction.commands.StoryCommand;
import id.naturalsmp.naturalinteraction.editor.EditorMode;
import id.naturalsmp.naturalinteraction.facts.FactsManager;
import id.naturalsmp.naturalinteraction.hook.CitizensHook;
import id.naturalsmp.naturalinteraction.listener.DungeonCompletionListener;
import id.naturalsmp.naturalinteraction.listener.EditorListener;
import id.naturalsmp.naturalinteraction.listener.InteractionListener;
import id.naturalsmp.naturalinteraction.listener.PrologueJoinListener;
import id.naturalsmp.naturalinteraction.listener.ScrollListener;
import id.naturalsmp.naturalinteraction.listener.QuestObjectiveListener;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.manifest.ManifestManager;
import id.naturalsmp.naturalinteraction.npc.StoryNPCManager;
import id.naturalsmp.naturalinteraction.story.StoryListener;
import id.naturalsmp.naturalinteraction.story.StoryManager;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import id.naturalsmp.naturalinteraction.utils.NaturalInteractionExpansion;
import id.naturalsmp.naturalinteraction.visual.ElementalEffectManager;
import id.naturalsmp.naturalinteraction.webpanel.WebPanelServer;
import id.naturalsmp.naturalinteraction.cinematic.CinematicManager;
import id.naturalsmp.naturalinteraction.database.DatabaseManager;
import id.naturalsmp.naturalinteraction.quest.QuestManager;
import id.naturalsmp.naturalinteraction.quest.QuestOverlay;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalInteraction extends JavaPlugin implements Listener {

    private static NaturalInteraction instance;

    private StoryManager storyManager;
    private StoryNPCManager npcManager;
    private InteractionManager interactionManager;
    private EditorMode editorMode;
    private PrologueJoinListener prologueJoinListener;
    private ElementalEffectManager elementalEffectManager;
    private FactsManager factsManager;
    private ManifestManager manifestManager;
    private WebPanelServer webPanelServer;
    private CinematicManager cinematicManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private QuestOverlay questOverlay;

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

        // Database (MySQL) — opt-in, jika mysql.enabled: true
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

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

        // Manifest system (Phase 4 — declarative audience/display)
        this.manifestManager = new ManifestManager(this);

        // Web Panel (Phase 5) — conditional start
        if (getConfig().getBoolean("webpanel.enabled", false)) {
            int port = getConfig().getInt("webpanel.port", 8585);
            this.webPanelServer = new WebPanelServer(this);
            this.webPanelServer.start(port);
        }

        // Cinematic system (Phase 6)
        this.cinematicManager = new CinematicManager(this);

        // Quest system
        this.questManager = new QuestManager(this);
        this.questOverlay = new QuestOverlay(this);

        // Register this class as listener for PlayerQuitEvent (manifest cleanup)
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info(
                ChatUtils.colorize("<gradient:#4facfe:#00f2fe>NaturalInteraction</gradient> <white>v"
                        + getDescription().getVersion() + " has been enabled!"));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (manifestManager != null) manifestManager.removePlayer(event.getPlayer());
        if (cinematicManager != null) cinematicManager.getPlayer().stop(event.getPlayer());
    }

    @Override
    public void onDisable() {
        if (editorMode != null)          editorMode.disableAll();
        if (storyManager != null)        storyManager.saveProgress();
        if (elementalEffectManager != null) elementalEffectManager.stop();
        if (manifestManager != null)     manifestManager.cleanup();
        if (webPanelServer != null)      webPanelServer.stop();
        if (cinematicManager != null)    cinematicManager.cleanup();
        if (databaseManager != null)     databaseManager.disconnect();
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
        
        // Quest objectives
        pm.registerEvents(new QuestObjectiveListener(this), this);

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
    }

    // ─── Plugin Reload ────────────────────────────────────────────────────────

    public void reloadPlugin() {
        reloadConfig();
        if (storyManager != null)           storyManager.loadNodes();
        if (interactionManager != null)     interactionManager.loadInteractions();
        if (elementalEffectManager != null) elementalEffectManager.reload();
        if (manifestManager != null)        manifestManager.reload();
        if (cinematicManager != null)       cinematicManager.reload();
        if (questManager != null)           questManager.loadAll();
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
    public ManifestManager getManifestManager()                 { return manifestManager; }
    public CinematicManager getCinematicManager()               { return cinematicManager; }
    public DatabaseManager getDatabaseManager()                 { return databaseManager; }
    public QuestManager getQuestManager()                       { return questManager; }
    public QuestOverlay getQuestOverlay()                       { return questOverlay; }
}
