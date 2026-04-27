package id.naturalsmp.naturalinteraction.facts;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified player state store — replaces TagTracker and CompletionTracker.
 *
 * Facts are key-value pairs persisted per-player.
 * Supported types: boolean, integer, float, string.
 *
 * Naming conventions (enforced by callers):
 *  - "interaction.<id>.completed"   → replaces CompletionTracker
 *  - "tag.<name>"                   → replaces TagTracker boolean tags
 *  - Custom: "coffeeman_coffees"    → numeric counters
 *
 * All writes are async to prevent main-thread I/O lag.
 */
public class FactsManager {

    private final NaturalInteraction plugin;
    private final File factsFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Thread-safe in-memory store: UUID → {factKey → value (as String)}
    private final Map<UUID, Map<String, String>> data = new ConcurrentHashMap<>();

    // Debounce: avoid saving every single set() call when many facts change at once
    private final Set<UUID> pendingSaves = Collections.synchronizedSet(new HashSet<>());

    public FactsManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.factsFolder = new File(plugin.getDataFolder(), "facts");
        if (!factsFolder.exists()) factsFolder.mkdirs();
        loadAll();
    }

    // ─── Boolean ──────────────────────────────────────────────────────────────

    public boolean getBoolean(UUID player, String key, boolean def) {
        String v = getRaw(player, key);
        return v != null ? Boolean.parseBoolean(v) : def;
    }

    public void setBoolean(UUID player, String key, boolean value) {
        setRaw(player, key, String.valueOf(value));
    }

    // ─── Integer ──────────────────────────────────────────────────────────────

    public int getInt(UUID player, String key, int def) {
        String v = getRaw(player, key);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    public void setInt(UUID player, String key, int value) {
        setRaw(player, key, String.valueOf(value));
    }

    /** Add {@code delta} to an integer fact (creates at 0 if missing). */
    public void addInt(UUID player, String key, int delta) {
        setInt(player, key, getInt(player, key, 0) + delta);
    }

    // ─── Float ────────────────────────────────────────────────────────────────

    public float getFloat(UUID player, String key, float def) {
        String v = getRaw(player, key);
        if (v == null) return def;
        try { return Float.parseFloat(v); } catch (NumberFormatException e) { return def; }
    }

    public void setFloat(UUID player, String key, float value) {
        setRaw(player, key, String.valueOf(value));
    }

    // ─── String ───────────────────────────────────────────────────────────────

    public String getString(UUID player, String key, String def) {
        String v = getRaw(player, key);
        return v != null ? v : def;
    }

    public void setString(UUID player, String key, String value) {
        setRaw(player, key, value);
    }

    // ─── Completion (replaces CompletionTracker) ──────────────────────────────

    public boolean hasCompleted(UUID player, String interactionId) {
        return getBoolean(player, completionKey(interactionId), false);
    }

    public void markCompleted(UUID player, String interactionId) {
        setBoolean(player, completionKey(interactionId), true);
    }

    public void resetCompletion(UUID player, String interactionId) {
        remove(player, completionKey(interactionId));
    }

    private String completionKey(String interactionId) {
        return "interaction." + interactionId + ".completed";
    }

    // ─── Tag compatibility (replaces TagTracker) ──────────────────────────────

    public boolean hasTag(UUID player, String tag) {
        return getBoolean(player, "tag." + tag, false);
    }

    public void addTag(UUID player, String tag) {
        setBoolean(player, "tag." + tag, true);
    }

    public void removeTag(UUID player, String tag) {
        remove(player, "tag." + tag);
    }

    // ─── General ──────────────────────────────────────────────────────────────

    public boolean hasFact(UUID player, String key) {
        Map<String, String> facts = data.get(player);
        return facts != null && facts.containsKey(key);
    }

    public void remove(UUID player, String key) {
        Map<String, String> facts = data.get(player);
        if (facts != null && facts.remove(key) != null) {
            scheduleAsyncSave(player);
        }
    }

    public void resetAll(UUID player) {
        data.remove(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(factsFolder, player + ".json");
            if (file.exists()) file.delete();
        });
    }

    /** Returns a snapshot of all facts for a player (for /ni facts and Web Panel). */
    public Map<String, String> getAll(UUID player) {
        return Collections.unmodifiableMap(data.getOrDefault(player, Collections.emptyMap()));
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private String getRaw(UUID player, String key) {
        Map<String, String> facts = data.get(player);
        return facts != null ? facts.get(key) : null;
    }

    private void setRaw(UUID player, String key, String value) {
        data.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(key, value);
        scheduleAsyncSave(player);
    }

    /**
     * Debounced async save — coalesces multiple setRaw() calls in the same tick
     * into a single file write 2 seconds later.
     */
    private void scheduleAsyncSave(UUID player) {
        if (pendingSaves.add(player)) {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                pendingSaves.remove(player);
                savePlayer(player);
            }, 40L); // 2 second debounce
        }
    }

    private void loadAll() {
        data.clear();
        File[] files = factsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        int loaded = 0;
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                try (FileReader reader = new FileReader(file)) {
                    Map<String, String> facts = gson.fromJson(reader, mapType);
                    if (facts != null) {
                        data.put(uuid, new ConcurrentHashMap<>(facts));
                        loaded++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Facts] Failed to load: " + file.getName() + " — " + e.getMessage());
            }
        }
        plugin.getLogger().info("[Facts] Loaded facts for " + loaded + " players.");
    }

    private void savePlayer(UUID player) {
        Map<String, String> snapshot = new HashMap<>(data.getOrDefault(player, Collections.emptyMap()));
        File file = new File(factsFolder, player + ".json");
        if (snapshot.isEmpty()) {
            if (file.exists()) file.delete();
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(snapshot, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("[Facts] Failed to save facts for " + player + ": " + e.getMessage());
        }
    }
}
