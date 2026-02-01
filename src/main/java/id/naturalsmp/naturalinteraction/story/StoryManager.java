package id.naturalsmp.naturalinteraction.story;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StoryManager {

    private final NaturalInteraction plugin;
    private final Map<String, StoryNode> storyNodes = new HashMap<>();
    private final Map<UUID, PlayerStoryData> playerProgress = new HashMap<>();
    private File progressFile;
    private FileConfiguration progressConfig;

    public StoryManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        loadNodes();
        loadProgress();
    }

    public void loadNodes() {
        plugin.saveResource("story.yml", false);
        File file = new File(plugin.getDataFolder(), "story.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        storyNodes.clear();
        ConfigurationSection section = config.getConfigurationSection("story");
        if (section != null) {
            for (String nodeId : section.getKeys(false)) {
                String title = config.getString("story." + nodeId + ".title", "Unknown Task");
                StoryNode node = new StoryNode(nodeId, title);
                node.setDescription(config.getStringList("story." + nodeId + ".description"));
                node.setNextNodeId(config.getString("story." + nodeId + ".next-node"));

                ConfigurationSection objectivesSec = config.getConfigurationSection("story." + nodeId + ".objectives");
                if (objectivesSec != null) {
                    for (String objKey : objectivesSec.getKeys(false)) {
                        String desc = config.getString("story." + nodeId + ".objectives." + objKey + ".description");
                        ObjectiveType type = ObjectiveType.valueOf(
                                config.getString("story." + nodeId + ".objectives." + objKey + ".type").toUpperCase());
                        String target = config.getString("story." + nodeId + ".objectives." + objKey + ".target");
                        int amount = config.getInt("story." + nodeId + ".objectives." + objKey + ".amount", 1);
                        node.addObjective(new StoryNode.StoryObjective(desc, type, target, amount));
                    }
                }
                storyNodes.put(nodeId, node);
            }
        }
    }

    private void loadProgress() {
        progressFile = new File(plugin.getDataFolder(), "progress.yml");
        if (!progressFile.exists()) {
            try {
                progressFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);

        playerProgress.clear();
        ConfigurationSection section = progressConfig.getConfigurationSection("players");
        if (section != null) {
            for (String uuidStr : section.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                String currentNodeId = progressConfig.getString("players." + uuidStr + ".current-node");
                playerProgress.put(uuid, new PlayerStoryData(uuid, currentNodeId));
            }
        }
    }

    public void saveProgress() {
        for (Map.Entry<UUID, PlayerStoryData> entry : playerProgress.entrySet()) {
            progressConfig.set("players." + entry.getKey().toString() + ".current-node",
                    entry.getValue().getCurrentNodeId());
        }
        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StoryNode getPlayerCurrentNode(Player player) {
        PlayerStoryData data = playerProgress.get(player.getUniqueId());
        if (data == null)
            return null;
        return storyNodes.get(data.getCurrentNodeId());
    }

    public void startStory(Player player, String nodeId) {
        playerProgress.put(player.getUniqueId(), new PlayerStoryData(player.getUniqueId(), nodeId));
        saveProgress();
    }

    public static class PlayerStoryData {
        private final UUID playerUUID;
        private String currentNodeId;

        public PlayerStoryData(UUID playerUUID, String currentNodeId) {
            this.playerUUID = playerUUID;
            this.currentNodeId = currentNodeId;
        }

        public String getCurrentNodeId() {
            return currentNodeId;
        }

        public void setCurrentNodeId(String id) {
            this.currentNodeId = id;
        }
    }
}
