package id.naturalsmp.naturalinteraction.cinematic;

import com.google.gson.*;
import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
public class CinematicManager implements Listener {

    private final NaturalInteraction plugin;
    private final File cinematicsFolder;
    private final Map<String, CinematicSequence> sequences = new HashMap<>();
    private final Map<UUID, CinematicEditorSession> editors = new ConcurrentHashMap<>();
    private final CinematicPlayer player;

    public CinematicManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.cinematicsFolder = new File(plugin.getDataFolder(), "cinematics");
        if (!cinematicsFolder.exists()) cinematicsFolder.mkdirs();
        this.player = new CinematicPlayer(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    public void saveSequence(CinematicSequence seq) {
        sequences.put(seq.getId(), seq);
        File file = new File(cinematicsFolder, seq.getId() + ".json");
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            JsonObject root = new JsonObject();
            root.addProperty("id", seq.getId());
            root.addProperty("loop", seq.isLoop());
            root.addProperty("lockMovement", seq.isLockPlayerMovement());
            root.addProperty("hideHUD", seq.isHideHUD());

            JsonArray points = new JsonArray();
            for (CameraPoint pt : seq.getPoints()) {
                JsonObject p = new JsonObject();
                p.addProperty("world", pt.getLocation().getWorld().getName());
                p.addProperty("x", pt.getLocation().getX());
                p.addProperty("y", pt.getLocation().getY());
                p.addProperty("z", pt.getLocation().getZ());
                p.addProperty("yaw", pt.getYaw());
                p.addProperty("pitch", pt.getPitch());
                p.addProperty("duration", pt.getDurationTicks());
                p.addProperty("easing", pt.getEasing().name());
                if (pt.getText() != null && !pt.getText().isEmpty()) p.addProperty("text", pt.getText());
                points.add(p);
            }
            root.add("points", points);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("[Cinematic] Failed to save " + seq.getId() + ": " + e.getMessage());
        }
    }

    public CinematicSequence getSequence(String id) { return sequences.get(id); }
    public Set<String> getSequenceIds() { return sequences.keySet(); }
    public CinematicPlayer getPlayer() { return player; }

    public void cleanup() {
        player.cleanup();
        for (CinematicEditorSession session : editors.values()) session.end();
        editors.clear();
    }

    public CinematicEditorSession getEditor(org.bukkit.entity.Player p) { return editors.get(p.getUniqueId()); }
    public void startEditor(org.bukkit.entity.Player p, String id) {
        if (editors.containsKey(p.getUniqueId())) return;
        CinematicSequence seq = getSequence(id);
        if (seq == null) seq = new CinematicSequence(id);
        CinematicEditorSession session = new CinematicEditorSession(plugin, p, seq);
        editors.put(p.getUniqueId(), session);
        session.start();
    }
    public void stopEditor(org.bukkit.entity.Player p) {
        CinematicEditorSession session = editors.remove(p.getUniqueId());
        if (session != null) {
            saveSequence(session.getSequence());
            session.end();
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        CinematicEditorSession session = editors.get(event.getPlayer().getUniqueId());
        if (session != null) {
            event.setCancelled(true);
            new id.naturalsmp.naturalinteraction.gui.PointListGUI(plugin, event.getPlayer(), session.getSequence()).open();
        }
    }

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction().name().startsWith("RIGHT_CLICK")) {
            CinematicEditorSession session = editors.get(event.getPlayer().getUniqueId());
            if (session != null) {
                event.setCancelled(true);
                java.util.List<CameraPoint> points = session.getSequence().getPoints();
                if (!points.isEmpty()) {
                    points.remove(points.size() - 1);
                    event.getPlayer().sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&c✖ Titik kamera terakhir dihapus."));
                } else {
                    event.getPlayer().sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&c✖ Tidak ada titik yang bisa dihapus."));
                }
            }
        }
    }

    @EventHandler
    public void onSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        if (player.isPlaying(event.getPlayer().getUniqueId())) {
            // Cancel sneak state change or force spectator target re-attach
            // In vanilla, client detaches from spectator target when sneaking.
            // We can't cancel the client's detach, so we re-attach them.
            if (event.isSneaking()) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.reattach(event.getPlayer());
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onDismount(org.spigotmc.event.entity.EntityDismountEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player p) {
            if (player.isPlaying(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
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
                String text = p.has("text") ? p.get("text").getAsString() : null;
                CameraPoint.EasingType easing;
                try { easing = CameraPoint.EasingType.valueOf(easingStr.toUpperCase()); }
                catch (Exception e) { easing = CameraPoint.EasingType.LINEAR; }

                seq.addPoint(new CameraPoint(loc, yaw, pitch, duration, easing, text));
            }
        }

        return seq;
    }
}
