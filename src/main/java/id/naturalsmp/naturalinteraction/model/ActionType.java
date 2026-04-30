package id.naturalsmp.naturalinteraction.model;

public enum ActionType {
    // ─── Movement & World ─────────────────────────────────────────────────────
    TELEPORT,       // value: "x,y,z,world[,yaw,pitch]"
    COMMAND,        // value: "command without slash" (%player%)
    EFFECT,         // value: "EFFECT_TYPE,duration,amplifier"
    SOUND,          // value: "SOUND_NAME,volume,pitch"
    TITLE,          // value: "Title;Subtitle"
    MESSAGE,        // value: "Message text"
    ACTIONBAR,      // value: "Message text"
    SCREENEFFECT,   // value: "effect color fadein stay fadeout freeze|nofreeze"
    ITEM,           // value: "MATERIAL|ia:id,amount[,name]"
    TAKE_ITEM,      // value: "MATERIAL|ia:id,amount"
    ZOOM,           // value: "true" / "false"
    INVISIBLE,      // value: "true" / "false"
    CINEMATIC,      // value: "sequence_id"

    // ─── Facts System (new) ───────────────────────────────────────────────────
    SET_FACT,           // value: "factKey,value"
    ADD_FACT,           // value: "factKey,delta"   (numeric increment)
    REMOVE_FACT,        // value: "factKey"
    JUMP_IF_FACT,       // value: "factKey,expectedValue,targetNodeId"
    JUMP_IF_NOT_FACT,   // value: "factKey,expectedValue,targetNodeId"
    JUMP_IF_FACT_GT,    // value: "factKey,threshold,targetNodeId"   (greater than)
    JUMP_IF_FACT_LT,    // value: "factKey,threshold,targetNodeId"   (less than)

    // ─── Item Branching ───────────────────────────────────────────────────────
    JUMP_IF_ITEM,       // value: "MATERIAL|ia:id,amount,targetNodeId"
    JUMP_IF_NOT_ITEM,   // value: "MATERIAL|ia:id,amount,targetNodeId"

    // ─── Legacy (deprecated — kept for backward compat, map to Facts internally) ─
    @Deprecated ADD_TAG,          // → SET_FACT "tag.<name>,true"
    @Deprecated REMOVE_TAG,       // → REMOVE_FACT "tag.<name>"
    @Deprecated JUMP_IF_TAG,      // → JUMP_IF_FACT "tag.<name>,true,nodeId"
    @Deprecated JUMP_IF_NOT_TAG,  // → JUMP_IF_NOT_FACT "tag.<name>,true,nodeId"

    // ─── Quest System ─────────────────────────────────────────────────────────
    QUEST_START,        // value: "questId"
    QUEST_ADVANCE,      // value: "questId,stageId"
    QUEST_COMPLETE,     // value: "questId"
}
