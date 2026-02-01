package id.naturalsmp.naturalinteraction.story;

import java.util.ArrayList;
import java.util.List;

public class StoryNode {
    private final String id;
    private String title;
    private List<String> description;
    private List<StoryObjective> objectives;
    private String nextNodeId;

    public StoryNode(String id, String title) {
        this.id = id;
        this.title = title;
        this.description = new ArrayList<>();
        this.objectives = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public List<StoryObjective> getObjectives() {
        return objectives;
    }

    public void addObjective(StoryObjective objective) {
        this.objectives.add(objective);
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public static class StoryObjective {
        private String description;
        private ObjectiveType type;
        private String targetId; // NPC UUID, Mob Type, or Material
        private int requiredAmount;
        private int currentAmount;

        public StoryObjective(String description, ObjectiveType type, String targetId, int amount) {
            this.description = description;
            this.type = type;
            this.targetId = targetId;
            this.requiredAmount = amount;
            this.currentAmount = 0;
        }

        public String getDescription() {
            return description;
        }

        public ObjectiveType getType() {
            return type;
        }

        public String getTargetId() {
            return targetId;
        }

        public int getRequiredAmount() {
            return requiredAmount;
        }

        public int getCurrentAmount() {
            return currentAmount;
        }

        public void setCurrentAmount(int amount) {
            this.currentAmount = amount;
        }

        public boolean isCompleted() {
            return currentAmount >= requiredAmount;
        }
    }
}
