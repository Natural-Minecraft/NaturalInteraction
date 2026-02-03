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
                PlayerStoryData data = new PlayerStoryData(uuid, currentNodeId);

                ConfigurationSection objProg = progressConfig
                        .getConfigurationSection("players." + uuidStr + ".objective-progress");
                if (objProg != null) {
                    for (String key : objProg.getKeys(false)) {
                        data.setObjectiveProgress(Integer.parseInt(key),
                                progressConfig.getInt("players." + uuidStr + ".objective-progress." + key));
                    }
                }
                playerProgress.put(uuid, data);
            }
        }
    }

    public void saveProgress() {
        for (Map.Entry<UUID, PlayerStoryData> entry : playerProgress.entrySet()) {
            String path = "players." + entry.getKey().toString();
            progressConfig.set(path + ".current-node", entry.getValue().getCurrentNodeId());
            progressConfig.set(path + ".objective-progress", null); // Clear
            for (Map.Entry<Integer, Integer> prog : entry.getValue().getObjectiveProgress().entrySet()) {
                progressConfig.set(path + ".objective-progress." + prog.getKey(), prog.getValue());
            }
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

    public PlayerStoryData getPlayerData(Player player) {
        return playerProgress.computeIfAbsent(player.getUniqueId(), k -> new PlayerStoryData(k, "start_node"));
    }

    public void startStory(Player player, String nodeId) {
        playerProgress.put(player.getUniqueId(), new PlayerStoryData(player.getUniqueId(), nodeId));
        saveProgress();
    }

    public void advanceStory(Player player) {
        PlayerStoryData data = getPlayerData(player);
        StoryNode current = storyNodes.get(data.getCurrentNodeId());
        if (current != null && current.getNextNodeId() != null) {
            data.setCurrentNodeId(current.getNextNodeId());
            data.getObjectiveProgress().clear();
            saveProgress();
            player.sendMessage(id.naturalsmp.naturalinteraction.utils.ChatUtils
                    .toComponent("<green>Quest Upgraded: <gold>" + storyNodes.get(current.getNextNodeId()).getTitle()));
        }
    }

    public static class PlayerStoryData {
        private final UUID playerUUID;
        private String currentNodeId;
        private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

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

        public Map<Integer, Integer> getObjectiveProgress() {
            return objectiveProgress;
        }

        public void setObjectiveProgress(int index, int amount) {
            objectiveProgress.put(index, amount);
        }
    }

    public java.util.Set<String> getStoryNodeIds() {
        return storyNodes.keySet();
    }
}
