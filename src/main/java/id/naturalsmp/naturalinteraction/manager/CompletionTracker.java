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

    /**
     * Check if a player has completed a specific interaction
     */
    public boolean hasCompleted(UUID player, String interactionId) {
        Set<String> completed = completionData.get(player);
        return completed != null && completed.contains(interactionId);
    }

    /**
     * Mark an interaction as completed for a player
     */
    public void markCompleted(UUID player, String interactionId) {
        completionData.computeIfAbsent(player, k -> new HashSet<>()).add(interactionId);
        savePlayer(player);
    }

    /**
     * Remove completion record (for admin reset)
     */
    public void resetCompletion(UUID player, String interactionId) {
        Set<String> completed = completionData.get(player);
        if (completed != null) {
            completed.remove(interactionId);
            savePlayer(player);
        }
    }

    /**
     * Reset all completions for a player
     */
    public void resetAll(UUID player) {
        completionData.remove(player);
        File file = new File(completionsFolder, player.toString() + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Load all completion data from disk
     */
    private void loadAll() {
        completionData.clear();
        File[] files = completionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return;

        Type setType = new TypeToken<Set<String>>() {
        }.getType();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                String uuidStr = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);
                Set<String> completed = gson.fromJson(reader, setType);
                if (completed != null) {
                    completionData.put(uuid, completed);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load completion data: " + file.getName());
            }
        }
        plugin.getLogger().info("Loaded completion data for " + completionData.size() + " players.");
    }

    /**
     * Save completion data for a specific player
     */
    private void savePlayer(UUID player) {
        Set<String> completed = completionData.get(player);
        if (completed == null || completed.isEmpty()) {
            // Delete file if no completions
            File file = new File(completionsFolder, player.toString() + ".json");
            if (file.exists())
                file.delete();
            return;
        }

        File file = new File(completionsFolder, player.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(completed, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save completion data for " + player);
            e.printStackTrace();
        }
    }
}
