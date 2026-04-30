package id.naturalsmp.naturalinteraction.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import id.naturalsmp.naturalinteraction.NaturalInteraction;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {
    private final NaturalInteraction plugin;
    private final File questsFolder;
    private final Map<String, Quest> quests = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public QuestManager(NaturalInteraction plugin) {
        this.plugin = plugin;
        this.questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists()) {
            questsFolder.mkdirs();
            try {
                plugin.saveResource("quests/tutorial.json", false);
            } catch (IllegalArgumentException e) {
                // Ignore if resource doesn't exist in jar
            }
        }
        loadAll();
    }

    public void loadAll() {
        quests.clear();
        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Quest quest = gson.fromJson(reader, Quest.class);
                if (quest != null && quest.getId() != null) {
                    quests.put(quest.getId(), quest);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[QuestManager] Failed to load: " + file.getName() + " - " + e.getMessage());
            }
        }
        plugin.getLogger().info("[QuestManager] Loaded " + loaded + " quests.");
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Collection<Quest> getQuests() {
        return quests.values();
    }

    // ─── State Tracking ───────────────────────────────────────────────────────

    public String getActiveQuest(UUID player) {
        return plugin.getFactsManager().getString(player, "active_quest", null);
    }

    public void setActiveQuest(UUID player, String questId) {
        plugin.getFactsManager().setString(player, "active_quest", questId);
    }

    public void clearActiveQuest(UUID player) {
        plugin.getFactsManager().remove(player, "active_quest");
    }

    public String getQuestState(UUID player, String questId) {
        return plugin.getFactsManager().getString(player, "quest." + questId + ".state", "UNSTARTED");
    }

    public void setQuestState(UUID player, String questId, String state) {
        plugin.getFactsManager().setString(player, "quest." + questId + ".state", state);
    }

    public String getQuestStage(UUID player, String questId) {
        return plugin.getFactsManager().getString(player, "quest." + questId + ".stage", null);
    }

    public void setQuestStage(UUID player, String questId, String stageId) {
        plugin.getFactsManager().setString(player, "quest." + questId + ".stage", stageId);
    }
}
