package id.naturalsmp.naturalinteraction.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Hooks into NaturalCore's LanguageManager via reflection or direct API 
 * to fetch localized messages for a specific player without creating a hard dependency.
 */
public class LanguageHook {

    private static boolean hooked = false;
    private static Object languageManager;
    private static Method getLanguageMethod;
    private static Method getRawMessageMethod;

    public static void init() {
        Plugin core = Bukkit.getPluginManager().getPlugin("NaturalCore");
        if (core != null && core.isEnabled()) {
            try {
                // NaturalCore.getInstance().getLanguageManager()
                Method getInstance = core.getClass().getMethod("getInstance");
                Object coreInstance = getInstance.invoke(null);
                
                Method getLangMgr = core.getClass().getMethod("getLanguageManager");
                languageManager = getLangMgr.invoke(coreInstance);
                
                if (languageManager != null) {
                    getLanguageMethod = languageManager.getClass().getMethod("getLanguage", UUID.class);
                    getRawMessageMethod = languageManager.getClass().getMethod("getRawMessage", String.class, String.class);
                    hooked = true;
                    Bukkit.getLogger().info("[NaturalInteraction] Successfully hooked into NaturalCore LanguageManager!");
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[NaturalInteraction] Failed to hook NaturalCore LanguageManager: " + e.getMessage());
            }
        }
    }

    /**
     * Replaces a lang:key string with the translated value.
     */
    public static String translate(Player player, String text) {
        if (text == null) return "";
        if (!hooked || !text.startsWith("lang:")) return text;

        try {
            String path = text.substring(5); // Remove "lang:"
            String langCode = (String) getLanguageMethod.invoke(languageManager, player.getUniqueId());
            String translated = (String) getRawMessageMethod.invoke(languageManager, langCode, path);
            
            if (translated != null && !translated.isEmpty()) {
                return translated;
            }
        } catch (Exception ignored) {}
        
        return text;
    }
}
