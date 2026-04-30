package id.naturalsmp.naturalinteraction.quest;

public class QuestStage {
    private String id;
    private String objective;
    private String targetLocation; // Format: "world,x,y,z"

    public QuestStage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getTargetLocation() { return targetLocation; }
    public void setTargetLocation(String targetLocation) { this.targetLocation = targetLocation; }
}
