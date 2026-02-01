package id.naturalsmp.naturalinteraction.model;

import java.io.Serializable;

public class Option implements Serializable {
    private String text;
    private String targetNodeId; // The ID of the dialogue node this option leads to

    public Option() {}

    public Option(String text, String targetNodeId) {
        this.text = text;
        this.targetNodeId = targetNodeId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }
}
