package id.naturalsmp.naturalinteraction.prologue;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import net.kyori.adventure.text.Component;
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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the Pre-Prologue "Login Screen" cinematic sequence.
 *
 * Phase 1 — Looping custom-image title screen until player presses Shift/Crouch.
 * Phase 2 — Player floats down from Y=226 to Y=201 with healing-aura particles.
 * Phase 3 — screeneffect BLACK fullscreen → prologue interaction starts (player stays at Y=201).
 *
 * Usage: PrologueCinematicHandler.start(plugin, player)
 */
public class PrologueCinematicHandler implements Listener {

    private static final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private final NaturalInteraction plugin;
    private final Player player;

    private final String titleChar;
    private final String titleLogoChar;
    private final int floatBlocks;
    private final double floatSpeed;
    private final Particle floatParticle;
    private final String prologueId;
    private final String prePrologueCinematicId;

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

        this.prologueId  = plugin.getConfig().getString("prologue.interaction-id", "prologue");
        this.prePrologueCinematicId = plugin.getConfig().getString("prologue.cinematic.pre-prologue-cinematic-id", "pre-prologue");
        this.titleChar   = plugin.getConfig().getString("prologue.cinematic.title-char", "\uE000");
        this.titleLogoChar = plugin.getConfig().getString("prologue.cinematic.title-logo-char", "\uE001");
        this.floatBlocks = plugin.getConfig().getInt("prologue.cinematic.float-down-blocks", 25);
        this.floatSpeed  = plugin.getConfig().getDouble("prologue.cinematic.float-down-speed", 0.04);

        String particleName = plugin.getConfig().getString("prologue.cinematic.float-particle", "TOTEM_OF_UNDYING");
        Particle p;
        try { p = Particle.valueOf(particleName.toUpperCase()); }
        catch (Exception e) { p = Particle.TOTEM_OF_UNDYING; }
        this.floatParticle = p;
    }

    // ─── Phase 1: Looping Title Screen ───────────────────────────────────────

    private void begin() {
        active.add(player.getUniqueId());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Freeze player: no movement, no jump, and prevent falling
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGravity(false);
        player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 254, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, false));
        player.sendActionBar(Component.empty());

        // Teleport to sky-spawn (Y=226 by default)
        String worldName = plugin.getConfig().getString("prologue.world", "story_sky");
        World w = Bukkit.getWorld(worldName);
        if (w == null) w = Bukkit.getWorlds().get(0);
        Location skySpawn = new Location(w,
                plugin.getConfig().getDouble("prologue.sky-spawn.x", 0.0),
                plugin.getConfig().getDouble("prologue.sky-spawn.y", 226.0),
                plugin.getConfig().getDouble("prologue.sky-spawn.z", 0.0),
                (float) plugin.getConfig().getDouble("prologue.sky-spawn.yaw", 0.0),
                (float) plugin.getConfig().getDouble("prologue.sky-spawn.pitch", 30.0));
        player.teleport(skySpawn);

        // Show transparent login screen indefinitely
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "screeneffect fullscreen_transparent transparent 10 999999999 10 freeze " + player.getName() + " " + titleChar);
    }

    // ─── Crouch → start float ─────────────────────────────────────────────────

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!event.isSneaking()) return; // press-down only, ignore release

        transitionToFloat();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!event.getPlayer().equals(player)) return;
        cleanup(false);
    }

    private void transitionToFloat() {
        HandlerList.unregisterAll(this);

        // Play subtle chime
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.9f);

        // Transition: Black fade in with Natural logo
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "screeneffect fullscreen BLACK 5 10 10 freeze " + player.getName() + " " + titleLogoChar);

        // Delay starting the cinematic by 5 ticks (when screen is fully black)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Release freeze
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);

                // Enable flight so we can push them downward manually
                player.setAllowFlight(true);
                player.setFlying(true);

                // Play the cinematic camera simultaneously
                id.naturalsmp.naturalinteraction.cinematic.CinematicSequence seq =
                        plugin.getCinematicManager().getSequence(prePrologueCinematicId);
                if (seq != null) {
                    plugin.getCinematicManager().getPlayer().play(player, seq);
                }

                // Phase 2: float down (or let cinematic handle position while we spawn particles)
                int totalTicks = (int) Math.round(floatBlocks / floatSpeed);
                new PrologueFloatTask(plugin, player, floatSpeed, totalTicks, floatParticle, PrologueCinematicHandler.this::triggerScreenEffect)
                        .runTaskTimer(plugin, 0L, 1L);
            }
        }.runTaskLater(plugin, 5L);
    }

    // ─── Float done → screeneffect → start prologue ────────────────────────────

    private void triggerScreenEffect() {
        // Stop flight — player is now at ~Y=201
        player.setFlying(false);
        player.setAllowFlight(false);

        // Use ScreenEffect plugin command for a proper black fade
        // Syntax: screeneffect fullscreen BLACK <fadeIn> <stay> <fadeOut> [freeze] <player>
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "screeneffect fullscreen BLACK 10 40 20 freeze " + player.getName());

        // Wait for fade-in (10 ticks) then start prologue; screeneffect manages its own fade-out
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanup(false); return; }
                
                // Reset player state
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setGravity(true);
                
                plugin.getInteractionManager().startInteraction(player, prologueId);
                cleanup(true);
            }
        }.runTaskLater(plugin, 15L); // slight overlap so prologue loads under the black screen
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    private void cleanup(boolean triggered) {
        active.remove(player.getUniqueId());
        HandlerList.unregisterAll(this);
        if (!triggered && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setGravity(true);
            player.clearTitle();
        }
    }
}
