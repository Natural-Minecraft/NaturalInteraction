package id.naturalsmp.naturalinteraction.prologue;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the Pre-Prologue "Login Screen" cinematic sequence.
 *
 * Phase 1 — Title screen (custom image from resource pack) loops until crouch.
 * Phase 2 — Player floats down slowly with healing aura particles.
 * Phase 3 — Blackscreen fade → teleport to Dewi NPC → start prologue interaction.
 *
 * Usage: PrologueCinematicHandler.start(plugin, player)
 */
public class PrologueCinematicHandler implements Listener {

    // Track players in the cinematic to prevent double-trigger
    private static final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private final NaturalInteraction plugin;
    private final Player player;

    // Config values
    private final String titleChar;
    private final int floatBlocks;
    private final double floatSpeed;
    private final Particle floatParticle;
    private final Location dewi;
    private final String prologueId;

    // Phase 1 task reference (so we can cancel it)
    private PrologueTitleTask titleTask;

    // ─────────────────────────────────────────────────────────────────────────

    public static void start(NaturalInteraction plugin, Player player) {
        if (active.contains(player.getUniqueId())) return;
        new PrologueCinematicHandler(plugin, player).begin();
    }

    public static boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private PrologueCinematicHandler(NaturalInteraction plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        this.prologueId   = plugin.getConfig().getString("prologue.interaction-id", "prologue");
        this.titleChar    = plugin.getConfig().getString("prologue.cinematic.title-char", "\uE000");
        this.floatBlocks  = plugin.getConfig().getInt("prologue.cinematic.float-down-blocks", 25);
        this.floatSpeed   = plugin.getConfig().getDouble("prologue.cinematic.float-down-speed", 0.04);

        String particleName = plugin.getConfig().getString("prologue.cinematic.float-particle", "TOTEM_OF_UNDYING");
        Particle p;
        try { p = Particle.valueOf(particleName.toUpperCase()); }
        catch (Exception e) { p = Particle.TOTEM_OF_UNDYING; }
        this.floatParticle = p;

        // Dewi / prologue NPC location
        String dewiWorld = plugin.getConfig().getString("prologue.dewi-location.world", "story_sky");
        World w = Bukkit.getWorld(dewiWorld);
        if (w == null) w = Bukkit.getWorlds().get(0);
        this.dewi = new Location(w,
                plugin.getConfig().getDouble("prologue.dewi-location.x", 100),
                plugin.getConfig().getDouble("prologue.dewi-location.y", 64),
                plugin.getConfig().getDouble("prologue.dewi-location.z", 100),
                (float) plugin.getConfig().getDouble("prologue.dewi-location.yaw", 180),
                (float) plugin.getConfig().getDouble("prologue.dewi-location.pitch", 0));
    }

    // ─── Phase 1: Title Screen ───────────────────────────────────────────────

    private void begin() {
        active.add(player.getUniqueId());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Lock player: freeze movement, remove gravity feel
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 254, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, false));

        // Hide actionbar / bossbar remnants
        player.sendActionBar(Component.empty());

        // Teleport to sky-spawn
        String world = plugin.getConfig().getString("prologue.world", "story_sky");
        World w = Bukkit.getWorld(world);
        if (w == null) w = Bukkit.getWorlds().get(0);
        Location skySpawn = new Location(w,
                plugin.getConfig().getDouble("prologue.sky-spawn.x", 0.5),
                plugin.getConfig().getDouble("prologue.sky-spawn.y", 200),
                plugin.getConfig().getDouble("prologue.sky-spawn.z", 0.5),
                (float) plugin.getConfig().getDouble("prologue.sky-spawn.yaw", 0),
                (float) plugin.getConfig().getDouble("prologue.sky-spawn.pitch", 30));
        player.teleport(skySpawn);

        // Start the looping title (every 2 seconds = 40 ticks)
        titleTask = new PrologueTitleTask(player, titleChar);
        titleTask.runTaskTimer(plugin, 0L, 40L);
    }

    // ─── Phase 1 → Phase 2: Crouch detected ─────────────────────────────────

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!event.isSneaking()) return; // only on press-down, not release

        transitionToFloat();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().equals(player)) return;
        cleanup(false);
    }

    private void transitionToFloat() {
        HandlerList.unregisterAll(this); // stop listening for crouch

        if (titleTask != null) titleTask.cancel();
        player.clearTitle(); // dismiss title screen

        // Play a gentle ambient sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);

        // Remove freeze effects
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        // Allow flight so we can manually move them downward
        player.setAllowFlight(true);
        player.setFlying(true);

        // Phase 2: float down
        int totalTicks = (int) Math.round(floatBlocks / floatSpeed);
        new PrologueFloatTask(player, floatSpeed, totalTicks, floatParticle, this::transitionToBlackscreen)
                .runTaskTimer(plugin, 2L, 1L);
    }

    // ─── Phase 2 → Phase 3: Blackscreen & Trigger ────────────────────────────

    private void transitionToBlackscreen() {
        // Re-register quit listener for cleanup guard
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                if (e.getPlayer().equals(player)) cleanup(false);
            }
        }, plugin);

        // Stop flight
        player.setFlying(false);
        player.setAllowFlight(false);

        // Show black fade title (full-width block chars fill screen → black)
        String blackLine = "§0" + "█".repeat(25);
        player.showTitle(Title.title(
                ChatUtils.toComponent(blackLine),
                ChatUtils.toComponent(blackLine),
                Title.Times.times(
                        Duration.ofMillis(800),  // fade in
                        Duration.ofMillis(1500), // stay
                        Duration.ofMillis(500)   // fade out (prologue will play over it)
                )
        ));

        // After fade-in (40 ticks), teleport + start prologue
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanup(false); return; }
                finishCinematic();
            }
        }.runTaskLater(plugin, 40L);
    }

    private void finishCinematic() {
        // Restore normal state
        player.setGameMode(GameMode.ADVENTURE); // or SURVIVAL — prologue will manage
        player.clearTitle();
        player.sendActionBar(Component.empty());

        // Teleport to Dewi NPC
        player.teleport(dewi);

        // Small delay then trigger the prologue interaction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanup(false); return; }
                plugin.getInteractionManager().startInteraction(player, prologueId);
                cleanup(true);
            }
        }.runTaskLater(plugin, 20L);
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    private void cleanup(boolean triggered) {
        active.remove(player.getUniqueId());
        HandlerList.unregisterAll(this);
        if (titleTask != null && !titleTask.isCancelled()) titleTask.cancel();
        if (!triggered && player.isOnline()) {
            // Player quit mid-cinematic — just restore what we can
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.setAllowFlight(false);
            player.clearTitle();
        }
    }
}
