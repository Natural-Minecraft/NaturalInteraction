package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInput implements Listener {
    private static final Map<UUID, Consumer<String>> pending = new HashMap<>();
    private static boolean registered = false;

    public static void capture(NaturalInteraction plugin, Player player, Consumer<String> callback) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new ChatInput(), plugin);
            registered = true;
        }
        pending.put(player.getUniqueId(), callback);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (pending.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> callback = pending.remove(event.getPlayer().getUniqueId());
            String message = event.getMessage();
            
            // Sync callback
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.accept(message);
                }
            }.runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(ChatInput.class));
        }
    }
}
