package id.naturalsmp.naturalinteraction.manifest.display;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a BossBar to the player while they are in the audience.
 *
 * JSON config:
 *  { "type": "boss_bar", "text": "&eWelcome to the Village!", "color": "YELLOW", "overlay": "PROGRESS" }
 */
public class BossBarDisplay implements ManifestDisplay {

    private final String text;
    private final BossBar.Color color;
    private final BossBar.Overlay overlay;

    // Track per-player BossBar instances
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();

    public BossBarDisplay(String text, String color, String overlay) {
        this.text = text;
        this.color = parseColor(color);
        this.overlay = parseOverlay(overlay);
    }

    @Override public String getType() { return "boss_bar"; }

    @Override
    public void show(Player player, NaturalInteraction plugin) {
        UUID uuid = player.getUniqueId();
        // If already shown, skip (avoid duplicates)
        if (activeBars.containsKey(uuid)) return;

        String resolved = text.replace("%player%", player.getName());
        BossBar bar = BossBar.bossBar(
                ChatUtils.toComponent(resolved), 1.0f, color, overlay);
        player.showBossBar(bar);
        activeBars.put(uuid, bar);
    }

    @Override
    public void hide(Player player, NaturalInteraction plugin) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null && player.isOnline()) player.hideBossBar(bar);
    }

    // Cleanup when manifest unloads
    public void hideAll() {
        activeBars.forEach((uuid, bar) -> {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.hideBossBar(bar);
        });
        activeBars.clear();
    }

    private static BossBar.Color parseColor(String c) {
        try { return BossBar.Color.valueOf(c.toUpperCase()); }
        catch (Exception e) { return BossBar.Color.YELLOW; }
    }

    private static BossBar.Overlay parseOverlay(String o) {
        try { return BossBar.Overlay.valueOf(o.toUpperCase()); }
        catch (Exception e) { return BossBar.Overlay.PROGRESS; }
    }
}
