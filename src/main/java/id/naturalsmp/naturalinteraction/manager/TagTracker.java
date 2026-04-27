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
 * Tracks arbitrary string tags for players to manage quest branching states.
 * Persisted as JSON files per-player in plugins/NaturalInteraction/tags/
 * All file writes are executed asynchronously to prevent main-thread I/O lag.
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

    public boolean hasTag(UUID player, String tag) {
        Set<String> tags = tagData.get(player);
        return tags != null && tags.contains(tag);
    }

    public void addTag(UUID player, String tag) {
        tagData.computeIfAbsent(player, k -> new HashSet<>()).add(tag);
        savePlayerAsync(player);
    }

    public void removeTag(UUID player, String tag) {
        Set<String> tags = tagData.get(player);
        if (tags != null && tags.remove(tag)) {
            savePlayerAsync(player);
        }
    }

    public Set<String> getTags(UUID player) {
        return tagData.getOrDefault(player, Collections.emptySet());
    }

    public void resetAll(UUID player) {
        tagData.remove(player);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(tagsFolder, player + ".json");
            if (file.exists()) file.delete();
        });
    }

    // ─── Internal I/O ─────────────────────────────────────────────────────────

    private void loadAll() {
        tagData.clear();
        File[] files = tagsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        Type setType = new TypeToken<Set<String>>() {}.getType();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                Set<String> tags = gson.fromJson(reader, setType);
                if (tags != null) tagData.put(uuid, tags);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load tag data: " + file.getName());
            }
        }
        plugin.getLogger().info("Loaded tag data for " + tagData.size() + " players.");
    }

    private void savePlayerAsync(UUID player) {
        Set<String> snapshot = new HashSet<>(tagData.getOrDefault(player, Collections.emptySet()));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(tagsFolder, player + ".json");
            if (snapshot.isEmpty()) {
                if (file.exists()) file.delete();
                return;
            }
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(snapshot, writer);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save tag data for " + player);
                e.printStackTrace();
            }
        });
    }
}
