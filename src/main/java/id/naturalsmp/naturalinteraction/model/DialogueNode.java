package id.naturalsmp.naturalinteraction.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DialogueNode implements Serializable {
    private String id;
    private String text;
    private List<Option> options = new ArrayList<>();
    private List<Action> actions = new ArrayList<>();
    private int durationSeconds = 5; // Default duration for bossbar
    private boolean skippable = true;

    // For linear conversations without choices, automatically go to this node
    private String nextNodeId;

    // Per-node rewards
    private boolean giveReward = false;
    private List<String> commandRewards = new ArrayList<>();
    private int delayBeforeNext = 20; // Ticks

    public DialogueNode() {
    }

    public DialogueNode(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isSkippable() {
        return skippable;
    }

    public void setSkippable(boolean skippable) {
        this.skippable = skippable;
    }

    public String getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public int getDelayBeforeNext() {
        return delayBeforeNext;
    }

    public void setDelayBeforeNext(int delayBeforeNext) {
        this.delayBeforeNext = delayBeforeNext;
    }

    public boolean isGiveReward() {
        return giveReward;
    }

    public void setGiveReward(boolean giveReward) {
        this.giveReward = giveReward;
    }

    public List<String> getCommandRewards() {
        return commandRewards != null ? commandRewards : new ArrayList<>();
    }

    public void setCommandRewards(List<String> commandRewards) {
        this.commandRewards = commandRewards;
    }
}
