package id.naturalsmp.naturalinteraction.manager;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.PostCompletionMode;
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
    private final File cooldownsFile;

    // Cooldown and One-time reward tracking
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final CompletionTracker completionTracker;

    public InteractionManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.interactionsFolder = new File(plugin.getDataFolder(), "interactions");
        if (!interactionsFolder.exists()) {
            interactionsFolder.mkdirs();
        }
        this.cooldownsFile = new File(plugin.getDataFolder(), "cooldowns.json");
        this.completionTracker = new CompletionTracker(plugin);
        loadInteractions();
        loadCooldowns();
    }

    private final com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    public void loadInteractions() {
        interactions.clear();
        if (!interactionsFolder.exists())
            return;

        File[] files = interactionsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return;

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

    private void loadCooldowns() {
        cooldowns.clear();
        if (!cooldownsFile.exists())
            return;

        try (java.io.FileReader reader = new java.io.FileReader(cooldownsFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<UUID, Map<String, Long>>>() {
            }.getType();
            Map<UUID, Map<String, Long>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                cooldowns.putAll(loaded);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load cooldowns.json");
            e.printStackTrace();
        }
    }

    private void saveCooldowns() {
        try (java.io.FileWriter writer = new java.io.FileWriter(cooldownsFile)) {
            gson.toJson(cooldowns, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save cooldowns.json");
            e.printStackTrace();
        }
    }

    public Interaction getInteraction(String id) {
        return interactions.get(id);
    }

    public boolean hasInteraction(String id) {
        return interactions.containsKey(id);
    }

    public java.util.Set<String> getInteractionIds() {
        return interactions.keySet();
    }

    public CompletionTracker getCompletionTracker() {
        return completionTracker;
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

        // Determine which root to use based on completion status
        boolean hasCompleted = completionTracker.hasCompleted(player.getUniqueId(), interactionId);
        if (hasCompleted && interaction.getPostCompletionMode() == PostCompletionMode.ALTERNATE_NODES
                && interaction.getPostCompletionRootNodeId() != null) {
            session.startFromNode(interaction.getPostCompletionRootNodeId());
        } else {
            session.start();
        }
    }

    public void endInteraction(UUID uuid) {
        InteractionSession session = activeSessions.remove(uuid);
        // Clean up if needed
    }

    public InteractionSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public void createInteraction(String name) {
        if (interactions.containsKey(name))
            return;
        Interaction interaction = new Interaction(name);
        saveInteraction(interaction);
    }

    public void deleteInteraction(String name) {
        interactions.remove(name);
    }

    public boolean isOnCooldown(UUID player, String interactionId) {
        if (!cooldowns.containsKey(player))
            return false;
        Map<String, Long> userCooldowns = cooldowns.get(player);
        if (!userCooldowns.containsKey(interactionId))
            return false;

        long expiry = userCooldowns.get(interactionId);
        if (System.currentTimeMillis() >= expiry) {
            userCooldowns.remove(interactionId);
            if (userCooldowns.isEmpty())
                cooldowns.remove(player);
            saveCooldowns();
            return false;
        }
        return true;
    }

    public long getCooldownRemaining(UUID player, String interactionId) {
        if (!isOnCooldown(player, interactionId))
            return 0;
        return cooldowns.get(player).get(interactionId) - System.currentTimeMillis();
    }

    public void setOnCooldown(UUID player, String interactionId, long durationSeconds) {
        if (durationSeconds <= 0)
            return;
        cooldowns.computeIfAbsent(player, k -> new HashMap<>()).put(interactionId,
                System.currentTimeMillis() + (durationSeconds * 1000L));
        saveCooldowns();
    }
}
