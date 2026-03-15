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
 * Tracks arbitrary string tags for players to manage quests and varied interaction states.
 * Persisted as JSON files per-player in plugins/NaturalInteraction/tags/
 */
public class TagTracker {
    private final NaturalInteraction plugin;
    private final File tagsFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // player UUID -> Set of tags
    private final Map<UUID, Set<String>> tagData = new HashMap<>();

    public TagTracker(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.tagsFolder = new File(plugin.getDataFolder(), "tags");
        if (!tagsFolder.exists()) {
            tagsFolder.mkdirs();
        }
        loadAll();
    }

    /**
     * Check if a player has a specific tag
     */
    public boolean hasTag(UUID player, String tag) {
        Set<String> tags = tagData.get(player);
        return tags != null && tags.contains(tag);
    }

    /**
     * Add a tag for a player
     */
    public void addTag(UUID player, String tag) {
        tagData.computeIfAbsent(player, k -> new HashSet<>()).add(tag);
        savePlayer(player);
    }

    /**
     * Remove a specific tag for a player
     */
    public void removeTag(UUID player, String tag) {
        Set<String> tags = tagData.get(player);
        if (tags != null) {
            tags.remove(tag);
            savePlayer(player);
        }
    }

    /**
     * Get all tags for a player
     */
    public Set<String> getTags(UUID player) {
        return tagData.getOrDefault(player, new HashSet<>());
    }

    /**
     * Reset all tags for a player
     */
    public void resetAll(UUID player) {
        tagData.remove(player);
        File file = new File(tagsFolder, player.toString() + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Load all tag data from disk
     */
    private void loadAll() {
        tagData.clear();
        File[] files = tagsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type setType = new TypeToken<Set<String>>() {}.getType();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                String uuidStr = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(uuidStr);
                Set<String> tags = gson.fromJson(reader, setType);
                if (tags != null) {
                    tagData.put(uuid, tags);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load tag data: " + file.getName());
            }
        }
        plugin.getLogger().info("Loaded tag data for " + tagData.size() + " players.");
    }

    /**
     * Save tag data for a specific player
     */
    private void savePlayer(UUID player) {
        Set<String> tags = tagData.get(player);
        if (tags == null || tags.isEmpty()) {
            // Delete file if no tags
            File file = new File(tagsFolder, player.toString() + ".json");
            if (file.exists()) file.delete();
            return;
        }

        File file = new File(tagsFolder, player.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(tags, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save tag data for " + player);
            e.printStackTrace();
        }
    }
}
