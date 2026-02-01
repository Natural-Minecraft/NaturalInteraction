package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractionManager {
    private final NaturalInteraction plugin;
    private final Map<String, Interaction> interactions = new HashMap<>(); // id -> Interaction
    private final Map<UUID, InteractionSession> activeSessions = new HashMap<>(); // player -> session
    private final File interactionsFolder;

    // Cooldown and One-time reward tracking
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    
    public InteractionManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.interactionsFolder = new File(plugin.getDataFolder(), "interactions");
        if (!interactionsFolder.exists()) {
            interactionsFolder.mkdirs();
        }
        loadInteractions();
    }

    private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    public void loadInteractions() {
        interactions.clear();
        if (!interactionsFolder.exists()) return;
        
        File[] files = interactionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        
        for (File file : files) {
            try (java.io.FileReader reader = new java.io.FileReader(file)) {
                Interaction interaction = gson.fromJson(reader, Interaction.class);
                if (interaction != null) {
                    interactions.put(interaction.getId(), interaction);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load interaction from " + file.getName());
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("Loaded " + interactions.size() + " interactions.");
    }

    public void saveInteraction(Interaction interaction) {
        interactions.put(interaction.getId(), interaction);
        File file = new File(interactionsFolder, interaction.getId() + ".json");
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            gson.toJson(interaction, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save interaction " + interaction.getId());
            e.printStackTrace();
        }
    }

    public Interaction getInteraction(String id) {
        return interactions.get(id);
    }
    
    public boolean hasInteraction(String id) {
        return interactions.containsKey(id);
    }
    
    /* Session Management */
    
    public void startInteraction(Player player, String interactionId) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in an interaction!");
            return;
        }
        
        Interaction interaction = getInteraction(interactionId);
        if (interaction == null) {
            player.sendMessage(ChatColor.RED + "Interaction not found: " + interactionId);
            return;
        }

        // Cooldown Check
        if (isOnCooldown(player.getUniqueId(), interactionId)) {
            player.sendMessage(ChatColor.RED + "Please wait before interacting again.");
            return;
        }

        InteractionSession session = new InteractionSession(plugin, player, interaction);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
    }
    
    public void endInteraction(UUID uuid) {
        InteractionSession session = activeSessions.remove(uuid);
        // Clean up if needed
    }

    public InteractionSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }
    
    public void createInteraction(String name) {
        if (interactions.containsKey(name)) return;
        Interaction interaction = new Interaction(name);
        saveInteraction(interaction);
    }
    
    public void deleteInteraction(String name) {
        interactions.remove(name);
    }

    public boolean isOnCooldown(UUID player, String interactionId) {
        // Simple cooldown logic
        return false;
    }
}