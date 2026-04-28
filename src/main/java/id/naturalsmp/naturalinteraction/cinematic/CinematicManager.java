package id.naturalsmp.naturalinteraction.cinematic;

import com.google.gson.*;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Manages cinematic sequences — loads from JSON, provides access, delegates playback.
 *
 * Cinematic JSON format (cinematics/<id>.json):
 * {
 *   "id": "prologue_intro",
 *   "loop": false,
 *   "lockMovement": true,
 *   "hideHUD": true,
 *   "points": [
 *     { "world": "story_sky", "x": 100, "y": 80, "z": 200, "yaw": 45, "pitch": -10, "duration": 100, "easing": "SMOOTH" },
 *     { "world": "story_sky", "x": 110, "y": 75, "z": 190, "yaw": 90, "pitch": 0, "duration": 60, "easing": "EASE_IN_OUT" }
 *   ]
 * }
 */
public class CinematicManager {

    private final NaturalInteraction plugin;
    private final File cinematicsFolder;
    private final Map<String, CinematicSequence> sequences = new HashMap<>();
    private final CinematicPlayer player;

    public CinematicManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.cinematicsFolder = new File(plugin.getDataFolder(), "cinematics");
        if (!cinematicsFolder.exists()) cinematicsFolder.mkdirs();
        this.player = new CinematicPlayer(plugin);
        loadAll();
    }

    public void loadAll() {
        sequences.clear();
        File[] files = cinematicsFolder.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                CinematicSequence seq = parseSequence(root);
                if (seq != null) sequences.put(seq.getId(), seq);
            } catch (Exception e) {
                plugin.getLogger().warning("[Cinematic] Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("[Cinematic] Loaded " + sequences.size() + " sequences.");
    }

    public void reload() {
        player.cleanup();
        loadAll();
    }

    public CinematicSequence getSequence(String id) { return sequences.get(id); }
    public Set<String> getSequenceIds() { return sequences.keySet(); }
    public CinematicPlayer getPlayer() { return player; }

    public void cleanup() {
        player.cleanup();
    }

    // ─── Parsing ──────────────────────────────────────────────────────────────

    private CinematicSequence parseSequence(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : null;
        if (id == null) return null;

        CinematicSequence seq = new CinematicSequence(id);
        seq.setLoop(json.has("loop") && json.get("loop").getAsBoolean());
        seq.setLockPlayerMovement(!json.has("lockMovement") || json.get("lockMovement").getAsBoolean());
        seq.setHideHUD(!json.has("hideHUD") || json.get("hideHUD").getAsBoolean());

        if (json.has("points")) {
            for (JsonElement el : json.getAsJsonArray("points")) {
                JsonObject p = el.getAsJsonObject();
                String worldName = p.has("world") ? p.get("world").getAsString() : "world";
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[Cinematic] Unknown world: " + worldName);
                    continue;
                }
                Location loc = new Location(world,
                        p.get("x").getAsDouble(),
                        p.get("y").getAsDouble(),
                        p.get("z").getAsDouble());
                float yaw = p.has("yaw") ? p.get("yaw").getAsFloat() : 0;
                float pitch = p.has("pitch") ? p.get("pitch").getAsFloat() : 0;
                int duration = p.has("duration") ? p.get("duration").getAsInt() : 40;
                String easingStr = p.has("easing") ? p.get("easing").getAsString() : "LINEAR";
                CameraPoint.EasingType easing;
                try { easing = CameraPoint.EasingType.valueOf(easingStr.toUpperCase()); }
                catch (Exception e) { easing = CameraPoint.EasingType.LINEAR; }

                seq.addPoint(new CameraPoint(loc, yaw, pitch, duration, easing));
            }
        }

        return seq;
    }
}
