package id.naturalsmp.naturalinteraction.listener;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.CompletionTracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forces all players to complete the prologue interaction before playing.
 * On join: if prologue not completed, save inventory & location, then teleport to story_sky.
 * After prologue completes, the player's inventory & location are restored.
 */
public class PrologueJoinListener implements Listener {

    private static final String PROLOGUE_ID = "prologue";
    private final NaturalInteraction plugin;
    private final File savedDataFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PrologueJoinListener(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.savedDataFolder = new File(plugin.getDataFolder(), "prologue_saves");
        if (!savedDataFolder.exists()) {
            savedDataFolder.mkdirs();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Skip admins
        if (player.hasPermission("naturalsmp.admin")) return;

        CompletionTracker tracker = plugin.getInteractionManager().getCompletionTracker();

        // Delay check by 1 second to let world load
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                if (!tracker.hasCompleted(player.getUniqueId(), PROLOGUE_ID)) {
                    // Player hasn't done prologue — save data and teleport to story_sky

                    // Only save if not already in story_sky AND no existing save
                    // (prevent overwriting good save with corrupted data on re-join)
                    if (!player.getWorld().getName().equalsIgnoreCase("quest_sky")
                            && !hasSavedData(player.getUniqueId())) {
                        savePlayerData(player);
                    }

                    // Teleport to quest_sky via Multiverse
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "mvtp " + player.getName() + " quest_sky");

                    player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                            .append(Component.text("Kamu harus menyelesaikan prologue terlebih dahulu!", NamedTextColor.YELLOW)));
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    /**
     * Save player's inventory and location before prologue
     */
    public void savePlayerData(Player player) {
        Map<String, Object> data = new HashMap<>();

        // Save location
        Location loc = player.getLocation();
        data.put("world", loc.getWorld().getName());
        data.put("x", loc.getX());
        data.put("y", loc.getY());
        data.put("z", loc.getZ());
        data.put("yaw", (double) loc.getYaw());
        data.put("pitch", (double) loc.getPitch());

        // Save inventory as base64
        data.put("inventory", serializeInventory(player.getInventory().getContents()));
        data.put("armor", serializeInventory(player.getInventory().getArmorContents()));

        File file = new File(savedDataFolder, player.getUniqueId().toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save prologue data for " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Restore player's inventory and location after prologue completion.
     * Returns true if data was restored.
     */
    public boolean restorePlayerData(Player player) {
        return restorePlayerData(player, true);
    }

    public boolean restorePlayerData(Player player, boolean teleport) {
        File file = new File(savedDataFolder, player.getUniqueId().toString() + ".json");
        if (!file.exists()) return false;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            if (data == null) return false;

            // Restore location
            String worldName = (String) data.get("world");
            double x = ((Number) data.get("x")).doubleValue();
            double y = ((Number) data.get("y")).doubleValue();
            double z = ((Number) data.get("z")).doubleValue();
            float yaw = ((Number) data.get("yaw")).floatValue();
            float pitch = ((Number) data.get("pitch")).floatValue();

            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null && teleport) {
                Location restoreLoc = new Location(world, x, y, z, yaw, pitch);
                player.teleport(restoreLoc);
            }

            // Restore inventory
            if (data.containsKey("inventory")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> invData = (java.util.List<Map<String, Object>>) data.get("inventory");
                ItemStack[] contents = deserializeInventory(invData);
                // Only restore if original was not empty OR player has nothing currently
                boolean originalWasEmpty = true;
                for (ItemStack item : contents) {
                    if (item != null) {
                        originalWasEmpty = false;
                        break;
                    }
                }
                
                if (!originalWasEmpty) {
                    player.getInventory().setContents(contents);
                }
            }

            if (data.containsKey("armor")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> armorData = (java.util.List<Map<String, Object>>) data.get("armor");
                ItemStack[] armor = deserializeInventory(armorData);
                player.getInventory().setArmorContents(armor);
            }

            // Delete save file
            file.delete();
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore prologue data for " + player.getName());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a player has saved prologue data (was forced into prologue)
     */
    public boolean hasSavedData(UUID uuid) {
        return new File(savedDataFolder, uuid.toString() + ".json").exists();
    }

    /**
     * Serialize inventory to a list of maps (Bukkit serialization)
     */
    private java.util.List<Map<String, Object>> serializeInventory(ItemStack[] contents) {
        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null) {
                list.add(item.serialize());
            } else {
                list.add(null);
            }
        }
        return list;
    }

    /**
     * Deserialize inventory from list of maps
     */
    private ItemStack[] deserializeInventory(java.util.List<Map<String, Object>> data) {
        if (data == null) return new ItemStack[0];
        ItemStack[] items = new ItemStack[data.size()];
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> map = data.get(i);
            if (map != null) {
                items[i] = ItemStack.deserialize(map);
            }
        }
        return items;
    }
}
