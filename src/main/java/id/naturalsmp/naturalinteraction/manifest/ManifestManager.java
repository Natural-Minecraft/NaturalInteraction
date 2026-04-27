package id.naturalsmp.naturalinteraction.manifest;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manifest.condition.*;
import id.naturalsmp.naturalinteraction.manifest.display.*;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ManifestManager loads declarative manifest entries from JSON files
 * in the manifests/ data folder and runs a ticker every 20 ticks
 * to evaluate conditions and apply displays.
 *
 * JSON format (manifests/village_welcome.json):
 * {
 *   "id": "village_welcome",
 *   "condition": { "type": "player_in_region", "world": "world", "x1": 0, "y1": 60, "z1": 0, "x2": 100, "y2": 80, "z2": 100 },
 *   "display": { "type": "boss_bar", "text": "&eWelcome to the Village!", "color": "GREEN" },
 *   "children": [
 *     {
 *       "id": "blacksmith_area",
 *       "condition": { "type": "fact_equals", "key": "quest_stage", "value": "find_blacksmith" },
 *       "display": { "type": "action_bar", "text": "&6➤ Talk to the Blacksmith!" }
 *     }
 *   ]
 * }
 */
public class ManifestManager {

    private final NaturalInteraction plugin;
    private final File manifestsFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<AudienceDisplay> entries = new ArrayList<>();
    private BukkitRunnable tickerTask;
    private final Logger log;

    public ManifestManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.manifestsFolder = new File(plugin.getDataFolder(), "manifests");
        if (!manifestsFolder.exists()) manifestsFolder.mkdirs();
        loadAll();
        startTicker();
    }

    // ─── Loading ──────────────────────────────────────────────────────────────

    public void loadAll() {
        cleanup(); // Cleanup existing
        entries.clear();

        File[] files = manifestsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            log.info("[Manifest] No manifest files found.");
            return;
        }

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                AudienceDisplay entry = parseEntry(root);
                if (entry != null) entries.add(entry);
            } catch (Exception e) {
                log.warning("[Manifest] Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
        log.info("[Manifest] Loaded " + entries.size() + " manifest entries.");
    }

    public void reload() {
        stopTicker();
        loadAll();
        startTicker();
    }

    // ─── Ticker ───────────────────────────────────────────────────────────────

    private void startTicker() {
        tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    for (AudienceDisplay entry : entries) {
                        entry.tick(player, plugin);
                    }
                }
            }
        };
        tickerTask.runTaskTimer(plugin, 20L, 20L); // Every 1 second
    }

    private void stopTicker() {
        if (tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
    }

    public void cleanup() {
        stopTicker();
        for (AudienceDisplay entry : entries) {
            entry.cleanup(plugin);
        }
    }

    /** Remove a player from all audiences (on disconnect). */
    public void removePlayer(Player player) {
        for (AudienceDisplay entry : entries) {
            entry.removePlayer(player, plugin);
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public List<AudienceDisplay> getEntries() { return Collections.unmodifiableList(entries); }

    // ─── JSON Parsing ─────────────────────────────────────────────────────────

    private AudienceDisplay parseEntry(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "unnamed";

        // Parse condition
        Condition condition = null;
        if (json.has("condition")) {
            condition = parseCondition(json.getAsJsonObject("condition"));
        }

        // Parse display
        ManifestDisplay display = null;
        if (json.has("display")) {
            display = parseDisplay(json.getAsJsonObject("display"));
        }

        // Parse children
        List<AudienceDisplay> children = new ArrayList<>();
        if (json.has("children")) {
            for (JsonElement child : json.getAsJsonArray("children")) {
                AudienceDisplay childEntry = parseEntry(child.getAsJsonObject());
                if (childEntry != null) children.add(childEntry);
            }
        }

        return new AudienceDisplay(id, condition, display, children);
    }

    private Condition parseCondition(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";

        return switch (type) {
            case "player_in_region" -> new PlayerInRegionCondition(
                    json.get("world").getAsString(),
                    json.get("x1").getAsDouble(), json.get("y1").getAsDouble(), json.get("z1").getAsDouble(),
                    json.get("x2").getAsDouble(), json.get("y2").getAsDouble(), json.get("z2").getAsDouble()
            );
            case "fact_equals" -> new FactEqualsCondition(
                    json.get("key").getAsString(),
                    json.get("value").getAsString()
            );
            case "fact_greater_than" -> new FactGreaterThanCondition(
                    json.get("key").getAsString(),
                    json.get("threshold").getAsFloat()
            );
            case "interaction_completed" -> new InteractionCompletedCondition(
                    json.get("interaction").getAsString()
            );
            case "player_has_permission" -> new PermissionCondition(
                    json.get("permission").getAsString()
            );
            default -> {
                log.warning("[Manifest] Unknown condition type: " + type);
                yield null;
            }
        };
    }

    private ManifestDisplay parseDisplay(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";

        return switch (type) {
            case "boss_bar" -> new BossBarDisplay(
                    json.get("text").getAsString(),
                    json.has("color") ? json.get("color").getAsString() : "YELLOW",
                    json.has("overlay") ? json.get("overlay").getAsString() : "PROGRESS"
            );
            case "action_bar" -> new ActionBarDisplay(json.get("text").getAsString());
            case "title" -> new TitleDisplay(
                    json.get("text").getAsString(),
                    json.has("subtitle") ? json.get("subtitle").getAsString() : ""
            );
            default -> {
                log.warning("[Manifest] Unknown display type: " + type);
                yield null;
            }
        };
    }
}
