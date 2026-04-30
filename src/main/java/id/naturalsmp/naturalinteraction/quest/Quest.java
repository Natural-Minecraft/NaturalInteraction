package id.naturalsmp.naturalinteraction.quest;

import java.util.LinkedHashMap;
import java.util.Map;

public class Quest {
    private String id;
    private String name;
    private String description;
    private Map<String, QuestStage> stages = new LinkedHashMap<>();

    public Quest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, QuestStage> getStages() { return stages; }
    public void setStages(Map<String, QuestStage> stages) { this.stages = stages; }
}
