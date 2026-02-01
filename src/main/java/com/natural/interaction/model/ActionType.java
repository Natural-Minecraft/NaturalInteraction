package com.natural.interaction.model;

public enum ActionType {
    TELEPORT, // value: "x,y,z,world,yaw,pitch"
    COMMAND, // value: "command without slash" (placeholder %player%)
    EFFECT, // value: "EFFECT_TYPE,duration,amplifier"
    SOUND, // value: "SOUND_NAME,volume,pitch"
    TITLE, // value: "Title;Subtitle"
    MESSAGE, // value: "Message text"
    ZOOM // value: "true" (on) or "false" (off)
}
