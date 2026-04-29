package id.naturalsmp.naturalinteraction.cinematic;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;

public class CinematicEditorSession {
    private final NaturalInteraction plugin;
    private final Player player;
    private final CinematicSequence sequence;
    private final GameMode originalMode;
    private final Location originalLoc;
    private BukkitRunnable actionbarTask;

    public CinematicEditorSession(NaturalInteraction plugin, Player player, CinematicSequence sequence) {
        this.plugin = plugin;
        this.player = player;
        this.sequence = sequence;
        this.originalMode = player.getGameMode();
        this.originalLoc = player.getLocation().clone();
    }

    public void start() {
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ChatUtils.toComponent("&a✔ Memasuki Editor Cinematic: &e" + sequence.getId()));
        player.sendMessage(ChatUtils.toComponent("&7- Terbang ke lokasi kamera."));
        player.sendMessage(ChatUtils.toComponent("&7- Tekan &e[F] &7(Swap Item) untuk menyimpan titik kamera."));
        player.sendMessage(ChatUtils.toComponent("&7- Ketik &e/ni cinematic save &7untuk keluar dan menyimpan."));

        actionbarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                player.sendActionBar(ChatUtils.toComponent("&6Editing &e" + sequence.getId() + " &8| &7Titik: &a" + sequence.getPoints().size() + " &8| &eTekan [F] tambah titik"));
            }
        };
        actionbarTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void end() {
        if (actionbarTask != null) actionbarTask.cancel();
        if (player.isOnline()) {
            player.setGameMode(originalMode);
            player.teleport(originalLoc);
            player.sendMessage(ChatUtils.toComponent("&a✔ Keluar dari Editor Cinematic."));
        }
    }

    public Player getPlayer() { return player; }
    public CinematicSequence getSequence() { return sequence; }
}
