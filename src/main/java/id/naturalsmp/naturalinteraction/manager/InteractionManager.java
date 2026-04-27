package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.PostCompletionMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class InteractionManager {

    private final NaturalInteraction plugin;
    private final Map<String, Interaction> interactions = new HashMap<>();
    private final Map<UUID, InteractionSession> activeSessions = new HashMap<>();
    private final File interactionsFolder;
    private final File chaptersFolder;
    private final File cooldownsFile;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    // Debounce: prevent saving cooldowns on every single cooldown check
    private boolean cooldownSavePending = false;

    public InteractionManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.interactionsFolder = new File(plugin.getDataFolder(), "interactions");
        if (!interactionsFolder.exists()) interactionsFolder.mkdirs();
        this.chaptersFolder = new File(plugin.getDataFolder(), "chapters");
        if (!chaptersFolder.exists()) chaptersFolder.mkdirs();
        this.cooldownsFile = new File(plugin.getDataFolder(), "cooldowns.json");
        loadInteractions();
        loadCooldowns();
    }

    // ─── Interaction Loading ───────────────────────────────────────────────────

    public void loadInteractions() {
        interactions.clear();
        // Load from legacy flat interactions/ folder
        loadFromFolder(interactionsFolder);
        // Load from chapters/ folder (recursive)
        loadFromFolderRecursive(chaptersFolder);
        plugin.getLogger().info("Loaded " + interactions.size() + " interactions.");
    }

    private void loadFromFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            loadSingleFile(file);
        }
    }

    private void loadFromFolderRecursive(File folder) {
        if (!folder.exists()) return;
        File[] entries = folder.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (entry.isDirectory()) {
                loadFromFolderRecursive(entry);
            } else if (entry.getName().endsWith(".json")) {
                loadSingleFile(entry);
            }
        }
    }

    private void loadSingleFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            Interaction interaction = gson.fromJson(reader, Interaction.class);
            if (interaction != null) {
                // Auto-derive chapter from relative path inside chapters/
                if (interaction.getChapter().isEmpty()) {
                    String chapterPath = deriveChapterPath(file);
                    if (!chapterPath.isEmpty()) interaction.setChapter(chapterPath);
                }
                interactions.put(interaction.getId(), interaction);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load interaction from " + file.getName() + ": " + e.getMessage());
        }
    }

    /** Derives chapter path from a file's path relative to chapters/ folder. */
    private String deriveChapterPath(File file) {
        try {
            String relative = chaptersFolder.toPath().relativize(file.getParentFile().toPath()).toString();
            return relative.replace(File.separator, ".");
        } catch (Exception e) {
            return "";
        }
    }

    public void saveInteraction(Interaction interaction) {
        interactions.put(interaction.getId(), interaction);
        File file = new File(interactionsFolder, interaction.getId() + ".json");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(interaction, writer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save interaction " + interaction.getId());
                e.printStackTrace();
            }
        });
    }

    // ─── Cooldown Persistence ─────────────────────────────────────────────────

    private void loadCooldowns() {
        cooldowns.clear();
        if (!cooldownsFile.exists()) return;

        try (FileReader reader = new FileReader(cooldownsFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<
                    Map<UUID, Map<String, Long>>>() {}.getType();
            Map<UUID, Map<String, Long>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                cooldowns.putAll(loaded);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load cooldowns.json");
            e.printStackTrace();
        }
    }

    /**
     * Schedule an async save with debounce — avoids disk I/O on every cooldown
     * check that triggers an expired-cooldown cleanup.
     */
    private void scheduleCooldownSave() {
        if (cooldownSavePending) return;
        cooldownSavePending = true;
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            cooldownSavePending = false;
            saveCooldownsAsync();
        }, 40L); // 2 seconds debounce
    }

    private void saveCooldownsAsync() {
        // Copy snapshot for thread-safety
        Map<UUID, Map<String, Long>> snapshot = new HashMap<>();
        cooldowns.forEach((uuid, map) -> snapshot.put(uuid, new HashMap<>(map)));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter writer = new FileWriter(cooldownsFile)) {
                gson.toJson(snapshot, writer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save cooldowns.json");
                e.printStackTrace();
            }
        });
    }

    // ─── Interaction Accessors ─────────────────────────────────────────────────

    public Interaction getInteraction(String id) {
        return interactions.get(id);
    }

    public boolean hasInteraction(String id) {
        return interactions.containsKey(id);
    }

    public java.util.Set<String> getInteractionIds() {
        return interactions.keySet();
    }

    /** @deprecated Use {@link id.naturalsmp.naturalinteraction.facts.FactsManager} directly. */
    @Deprecated
    public CompletionTracker getCompletionTracker() {
        // Shim: return a wrapper that delegates to FactsManager
        return new CompletionTracker(plugin) {
            @Override public boolean hasCompleted(UUID p, String id) { return plugin.getFactsManager().hasCompleted(p, id); }
            @Override public void markCompleted(UUID p, String id)   { plugin.getFactsManager().markCompleted(p, id); }
            @Override public void resetCompletion(UUID p, String id) { plugin.getFactsManager().resetCompletion(p, id); }
        };
    }

    /** @deprecated Use {@link id.naturalsmp.naturalinteraction.facts.FactsManager} directly. */
    @Deprecated
    public TagTracker getTagTracker() {
        return new TagTracker(plugin) {
            @Override public boolean hasTag(UUID p, String tag) { return plugin.getFactsManager().hasTag(p, tag); }
            @Override public void addTag(UUID p, String tag)    { plugin.getFactsManager().addTag(p, tag); }
            @Override public void removeTag(UUID p, String tag) { plugin.getFactsManager().removeTag(p, tag); }
        };
    }

    // ─── Session Management ────────────────────────────────────────────────────

    public void startInteraction(Player player, String interactionId) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Kamu sedang dalam percakapan lain!", NamedTextColor.RED));
            return;
        }

        Interaction interaction = getInteraction(interactionId);
        if (interaction == null) {
            player.sendMessage(Component.text("Interaction tidak ditemukan: " + interactionId, NamedTextColor.RED));
            return;
        }

        if (isOnCooldown(player.getUniqueId(), interactionId)) {
            player.sendMessage(Component.text("Harap tunggu sebelum berinteraksi lagi.", NamedTextColor.RED));
            return;
        }

        int maxPlayers = interaction.getMaxConcurrentPlayers();
        if (maxPlayers > 0 && getActiveSessionCountForInteraction(interactionId) >= maxPlayers) {
            player.sendMessage(Component.text("Mohon antri, ada player lain sedang berinteraksi.", NamedTextColor.RED));
            return;
        }

        InteractionSession session = new InteractionSession(plugin, player, interaction);
        activeSessions.put(player.getUniqueId(), session);

        boolean hasCompleted = plugin.getFactsManager().hasCompleted(player.getUniqueId(), interactionId);
        if (hasCompleted
                && interaction.getPostCompletionMode() == PostCompletionMode.ALTERNATE_NODES
                && interaction.getPostCompletionRootNodeId() != null) {
            session.startFromNode(interaction.getPostCompletionRootNodeId());
        } else {
            session.start();
        }
    }

    public void endInteraction(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public InteractionSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public long getActiveSessionCountForInteraction(String interactionId) {
        return activeSessions.values().stream()
                .filter(s -> s.getInteraction().getId().equals(interactionId))
                .count();
    }

    public void createInteraction(String name) {
        if (interactions.containsKey(name)) return;
        Interaction interaction = new Interaction(name);
        saveInteraction(interaction);
    }

    /**
     * Deletes an interaction from memory AND removes its JSON file from disk.
     * Previously only removed from memory — file persisted and reloaded on restart.
     */
    public void deleteInteraction(String name) {
        interactions.remove(name);
        File file = new File(interactionsFolder, name + ".json");
        if (file.exists()) {
            file.delete();
            plugin.getLogger().info("Deleted interaction file: " + name + ".json");
        }
    }

    // ─── Cooldown API ─────────────────────────────────────────────────────────

    public boolean isOnCooldown(UUID player, String interactionId) {
        Map<String, Long> userCooldowns = cooldowns.get(player);
        if (userCooldowns == null) return false;

        Long expiry = userCooldowns.get(interactionId);
        if (expiry == null) return false;

        if (System.currentTimeMillis() >= expiry) {
            userCooldowns.remove(interactionId);
            if (userCooldowns.isEmpty()) cooldowns.remove(player);
            scheduleCooldownSave(); // Debounced async save
            return false;
        }
        return true;
    }

    public long getCooldownRemaining(UUID player, String interactionId) {
        if (!isOnCooldown(player, interactionId)) return 0;
        return cooldowns.get(player).get(interactionId) - System.currentTimeMillis();
    }

    public void setOnCooldown(UUID player, String interactionId, long durationSeconds) {
        if (durationSeconds <= 0) return;
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                .put(interactionId, System.currentTimeMillis() + (durationSeconds * 1000L));
        scheduleCooldownSave();
    }
}
