package id.naturalsmp.naturalinteraction.visual;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages elemental visual effects for NPC avatars.
 * Reads config from elemental_effects.yml and runs a repeating task
 * to render particles around configured NPCs.
 */
public class ElementalEffectManager {

    private final NaturalInteraction plugin;
    private final Map<Integer, ElementalEffect> effectMap = new HashMap<>(); // NPC ID -> Effect
    private final Map<Integer, Location> baseLocations = new HashMap<>(); // NPC ID -> base loc (for Wind floating)
    private BukkitTask renderTask;
    private long tickCounter = 0;

    public ElementalEffectManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        loadConfig();
        startRenderTask();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "elemental_effects.yml");
        if (!file.exists()) {
            plugin.saveResource("elemental_effects.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        effectMap.clear();
        baseLocations.clear();

        ConfigurationSection effects = config.getConfigurationSection("effects");
        if (effects == null) return;

        for (String key : effects.getKeys(false)) {
            ConfigurationSection section = effects.getConfigurationSection(key);
            if (section == null) continue;

            int npcId = section.getInt("npc-id", -1);
            if (npcId < 0) continue; // Not configured yet

            String type = section.getString("type", "").toUpperCase();
            ElementalEffect effect = switch (type) {
                case "WIND" -> new WindEffect(
                        section.getDouble("float-height", 3.0),
                        section.getDouble("bob-amplitude", 1.0),
                        section.getDouble("bob-period", 3.0)
                );
                case "EARTH" -> new EarthEffect(
                        section.getDouble("ground-radius", 2.5)
                );
                case "WATER" -> new WaterEffect(
                        section.getDouble("ground-radius", 2.5)
                );
                case "FIRE" -> new FireEffect(
                        section.getDouble("spiral-height", 2.5)
                );
                default -> null;
            };

            if (effect != null) {
                effectMap.put(npcId, effect);
                plugin.getLogger().info("Elemental effect '" + type + "' registered for NPC #" + npcId);
            }
        }
    }

    private void startRenderTask() {
        renderTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Wrap tick to prevent long overflow & floating-point precision loss in sin()
                tickCounter = (tickCounter + 1) % 1_000_000L;

                for (Map.Entry<Integer, ElementalEffect> entry : effectMap.entrySet()) {
                    int npcId = entry.getKey();
                    ElementalEffect effect = entry.getValue();

                    NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                    if (npc == null || !npc.isSpawned()) continue;

                    Location npcLoc = npc.getStoredLocation();
                    if (npcLoc == null || npcLoc.getWorld() == null) continue;

                    // Check if any players are nearby (performance optimization)
                    if (npcLoc.getWorld().getNearbyPlayers(npcLoc, 48).isEmpty()) continue;

                    // For Wind effect: handle floating NPC location
                    if (effect instanceof WindEffect windEffect) {
                        // Store base location on first encounter
                        if (!baseLocations.containsKey(npcId)) {
                            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "elemental_effects.yml"));
                            org.bukkit.configuration.ConfigurationSection effects = cfg.getConfigurationSection("effects");
                            Location configBaseLoc = null;
                            if (effects != null) {
                                for (String key : effects.getKeys(false)) {
                                    if (effects.getInt(key + ".npc-id") == npcId) {
                                        if (effects.contains(key + ".base-location")) {
                                            configBaseLoc = effects.getLocation(key + ".base-location");
                                        } else {
                                            configBaseLoc = npcLoc.clone();
                                            effects.set(key + ".base-location", configBaseLoc);
                                            try { cfg.save(new File(plugin.getDataFolder(), "elemental_effects.yml")); } catch (Exception ignored) {}
                                        }
                                        break;
                                    }
                                }
                            }
                            if (configBaseLoc == null) configBaseLoc = npcLoc.clone();
                            baseLocations.put(npcId, configBaseLoc);
                        }

                        Location baseLoc = baseLocations.get(npcId);
                        Location adjustedLoc = windEffect.getAdjustedNPCLocation(baseLoc, tickCounter);

                        // Teleport NPC to floating position
                        npc.teleport(adjustedLoc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);

                        // Render particles at adjusted location
                        effect.render(adjustedLoc, tickCounter);
                    } else {
                        // Normal ground effect
                        effect.render(npcLoc, tickCounter);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 2L); // Start after 1 second, run every 2 ticks
    }

    public void stop() {
        if (renderTask != null) {
            renderTask.cancel();
            renderTask = null;
        }

        // Reset Wind NPCs to base locations
        for (Map.Entry<Integer, Location> entry : baseLocations.entrySet()) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(entry.getKey());
            if (npc != null && npc.isSpawned()) {
                npc.teleport(entry.getValue(), org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }

        effectMap.values().forEach(ElementalEffect::cleanup);
        effectMap.clear();
        baseLocations.clear();
    }

    public void reload() {
        stop();
        loadConfig();
        startRenderTask();
    }
}
