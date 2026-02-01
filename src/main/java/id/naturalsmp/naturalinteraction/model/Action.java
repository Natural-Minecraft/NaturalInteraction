package id.naturalsmp.naturalinteraction.model;

import java.io.Serializable;

public class Action implements Serializable {
    private ActionType type;
    private String value;

    public Action() {}

    public Action(ActionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}