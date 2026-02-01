package id.naturalsmp.naturalinteraction;

import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalInteraction extends JavaPlugin {

    private static NaturalInteraction instance;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        saveDefaultConfig();

        // Metrics or other utils here
        
        getLogger().info(ChatUtils.colorize("<gradient:#4facfe:#00f2fe>NaturalInteraction</gradient> <white>has been enabled!"));
    }

    @Override
    public void onDisable() {
        // Shutdown logic
    }

    public static NaturalInteraction getInstance() {
        return instance;
    }
}
