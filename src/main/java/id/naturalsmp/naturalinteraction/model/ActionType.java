package id.naturalsmp.naturalinteraction.model;

public enum ActionType {
    TELEPORT, // value: "x,y,z,world,yaw,pitch"
    COMMAND, // value: "command without slash" (placeholder %player%)
    EFFECT, // value: "EFFECT_TYPE,duration,amplifier"
    SOUND, // value: "SOUND_NAME,volume,pitch"
    TITLE, // value: "Title;Subtitle"
    MESSAGE, // value: "Message text"
    ZOOM, // value: "true" (on) or "false" (off)
    ITEM, // value: "MATERIAL,amount,name"
    ACTIONBAR, // value: "Message text"
    SCREENEFFECT, // value: "effect color fadein stay fadeout freeze|nofreeze"
    INVISIBLE, // value: "true" (on) or "false" (off)
    ADD_TAG, // value: "tag_name"
    REMOVE_TAG, // value: "tag_name"
    JUMP_IF_TAG, // value: "tag_name,target_node_id"
    JUMP_IF_NOT_TAG, // value: "tag_name,target_node_id"
    TAKE_ITEM, // value: "material|ia_id,amount"
    JUMP_IF_ITEM, // value: "material|ia_id,amount,target_node_id"
    JUMP_IF_NOT_ITEM // value: "material|ia_id,amount,target_node_id"
}
