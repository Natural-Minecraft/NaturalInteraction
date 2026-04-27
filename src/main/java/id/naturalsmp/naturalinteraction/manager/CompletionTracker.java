package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Tracks which players have completed which interactions.
 * Persisted as JSON files per-player in plugins/NaturalInteraction/completions/
 * All file writes are executed asynchronously to prevent main-thread I/O lag.
 */
public class CompletionTracker {

    private final NaturalInteraction plugin;
    private final File completionsFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // player UUID -> Set of completed interaction IDs
    private final Map<UUID, Set<String>> completionData = new HashMap<>();

    public CompletionTracker(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.completionsFolder = new File(plugin.getDataFolder(), "completions");
        if (!completionsFolder.exists()) {
            completionsFolder.mkdirs();
        }
        loadAll();
    }

    public boolean hasCompleted(UUID player, String interactionId) {
        Set<String> completed = completionData.get(player);
        return completed != null && completed.contains(interactionId);
    }

    public void markCompleted(UUID player, String interactionId) {
        completionData.computeIfAbsent(player, k -> new HashSet<>()).add(interactionId);
        savePlayerAsync(player);
    }

    public void resetCompletion(UUID player, String interactionId) {
        Set<String> completed = completionData.get(player);
        if (completed != null) {
            completed.remove(interactionId);
            savePlayerAsync(player);
        }
    }

    public void resetAll(UUID player) {
        completionData.remove(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(completionsFolder, player + ".json");
            if (file.exists()) file.delete();
        });
    }

    public Set<String> getCompleted(UUID player) {
        return completionData.getOrDefault(player, Collections.emptySet());
    }

    // ─── Internal I/O ─────────────────────────────────────────────────────────

    private void loadAll() {
        completionData.clear();
        File[] files = completionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type setType = new TypeToken<Set<String>>() {}.getType();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                Set<String> completed = gson.fromJson(reader, setType);
                if (completed != null) completionData.put(uuid, completed);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load completion data: " + file.getName());
            }
        }
        plugin.getLogger().info("Loaded completion data for " + completionData.size() + " players.");
    }

    private void savePlayerAsync(UUID player) {
        Set<String> snapshot = new HashSet<>(completionData.getOrDefault(player, Collections.emptySet()));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(completionsFolder, player + ".json");
            if (snapshot.isEmpty()) {
                if (file.exists()) file.delete();
                return;
            }
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(snapshot, writer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save completion data for " + player);
                e.printStackTrace();
            }
        });
    }
}
