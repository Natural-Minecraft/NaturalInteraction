package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;

/**
 * Centralized configuration accessor.
 * Eliminates hardcoded values scattered throughout the codebase.
 * Always reads from the live config — reflects /ni reload changes.
 */
public final class PluginConfig {

    private PluginConfig() {}

    // ─── Prologue ─────────────────────────────────────────────────────────────

    public static String getPrologueInteractionId(NaturalInteraction plugin) {
        return plugin.getConfig().getString("prologue.interaction-id", "prologue");
    }

    public static int getPrologueNpcId(NaturalInteraction plugin) {
        return plugin.getConfig().getInt("prologue.npc-id", 69);
    }

    public static String getPrologueWorld(NaturalInteraction plugin) {
        return plugin.getConfig().getString("prologue.world", "story_sky");
    }

    public static String getPrologueNpcHologramDefault(NaturalInteraction plugin) {
        return plugin.getConfig().getString("prologue.npc-hologram-default", "§8§l????");
    }

    public static String getPrologueNpcHologramExclamation(NaturalInteraction plugin) {
        return plugin.getConfig().getString("prologue.npc-hologram-exclamation", "§b§l!");
    }

    // ─── Plugin Commands ───────────────────────────────────────────────────────

    public static String getMultiverseTeleportCommand(NaturalInteraction plugin) {
        return plugin.getConfig().getString("plugins.multiverse-teleport-command", "mvtp");
    }

    public static String getSudoCommand(NaturalInteraction plugin) {
        return plugin.getConfig().getString("plugins.sudo-command", "sudo");
    }

    // ─── General ──────────────────────────────────────────────────────────────

    public static boolean isDebug(NaturalInteraction plugin) {
        return plugin.getConfig().getBoolean("settings.debug", false);
    }

    public static double getDefaultRange(NaturalInteraction plugin) {
        return plugin.getConfig().getDouble("interaction.default-range", 3.0);
    }
}
