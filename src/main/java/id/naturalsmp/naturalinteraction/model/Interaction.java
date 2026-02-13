package id.naturalsmp.naturalinteraction.model;

import org.bukkit.inventory.ItemStack;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Interaction implements Serializable {
    private String id; // unique name
    private String rootNodeId;
    private Map<String, DialogueNode> nodes = new HashMap<>(); // id -> node

    private long cooldownSeconds = 0;
    private boolean oneTimeReward = false;
    private PostCompletionMode postCompletionMode = PostCompletionMode.SAME_NODES;
    private String postCompletionRootNodeId; // Alternate root for returning players
    private List<ItemStack> rewards = new ArrayList<>(); // Serialized separately usually, but for now we'll assumes
                                                         // bukkit serialization or transient

    public Interaction() {
    }

    public Interaction(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRootNodeId() {
        return rootNodeId;
    }

    public void setRootNodeId(String rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    public Map<String, DialogueNode> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, DialogueNode> nodes) {
        this.nodes = nodes;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public boolean isOneTimeReward() {
        return oneTimeReward;
    }

    public void setOneTimeReward(boolean oneTimeReward) {
        this.oneTimeReward = oneTimeReward;
    }

    public PostCompletionMode getPostCompletionMode() {
        return postCompletionMode != null ? postCompletionMode : PostCompletionMode.SAME_NODES;
    }

    public void setPostCompletionMode(PostCompletionMode postCompletionMode) {
        this.postCompletionMode = postCompletionMode;
    }

    public String getPostCompletionRootNodeId() {
        return postCompletionRootNodeId;
    }

    public void setPostCompletionRootNodeId(String postCompletionRootNodeId) {
        this.postCompletionRootNodeId = postCompletionRootNodeId;
    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    public void setRewards(List<ItemStack> rewards) {
        this.rewards = rewards;
    }

    public void addNode(DialogueNode node) {
        this.nodes.put(node.getId(), node);
    }

    public DialogueNode getNode(String nodeId) {
        return this.nodes.get(nodeId);
    }

    private List<String> commandRewards = new ArrayList<>();

    public List<String> getCommandRewards() {
        return commandRewards;
    }

    public void setCommandRewards(List<String> commandRewards) {
        this.commandRewards = commandRewards;
    }
}
