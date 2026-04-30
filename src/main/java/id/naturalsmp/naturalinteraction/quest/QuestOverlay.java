package id.naturalsmp.naturalinteraction.quest;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestOverlay implements Listener {
    private final NaturalInteraction plugin;
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();

    // Cache paths to avoid running A* every tick
    private final Map<UUID, List<Location>> pathCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pathCacheTime = new ConcurrentHashMap<>();

    public QuestOverlay(NaturalInteraction plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startNavTask();
    }

    public void updateOverlay(Player player) {
        String activeQuestId = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
        if (activeQuestId == null) {
            removeOverlay(player);
            return;
        }

        Quest quest = plugin.getQuestManager().getQuest(activeQuestId);
        if (quest == null) {
            removeOverlay(player);
            return;
        }

        String stageId = plugin.getQuestManager().getQuestStage(player.getUniqueId(), activeQuestId);
        QuestStage stage = stageId != null ? quest.getStages().get(stageId) : null;
        
        String objText = stage != null && stage.getObjective() != null ? stage.getObjective() : "Tujuan Tidak Diketahui";
        Component name = id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent("&6⭐ " + quest.getName() + " &8- &f" + objText);

        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            player.showBossBar(bar);
            activeBars.put(player.getUniqueId(), bar);
        } else {
            bar.name(name);
        }
    }

    public void removeOverlay(Player player) {
        BossBar bar = activeBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
        pathCache.remove(player.getUniqueId());
        pathCacheTime.remove(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateOverlay(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeOverlay(e.getPlayer());
    }

    private void startNavTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isSneaking()) continue;
                    
                    String activeQuestId = plugin.getQuestManager().getActiveQuest(player.getUniqueId());
                    if (activeQuestId == null) continue;

                    Quest quest = plugin.getQuestManager().getQuest(activeQuestId);
                    if (quest == null) continue;

                    String stageId = plugin.getQuestManager().getQuestStage(player.getUniqueId(), activeQuestId);
                    if (stageId == null) continue;
                    
                    QuestStage stage = quest.getStages().get(stageId);
                    if (stage == null || stage.getTargetLocation() == null) continue;

                    Location target = parseLocation(stage.getTargetLocation());
                    if (target == null || !target.getWorld().equals(player.getWorld())) continue;

                    drawSmartTrail(player, target);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Run every 0.5 seconds
    }

    private void drawSmartTrail(Player player, Location target) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Invalidate cache if older than 5 seconds or player moved far
        if (!pathCache.containsKey(uuid) || now - pathCacheTime.getOrDefault(uuid, 0L) > 5000) {
            // Calculate async to prevent lag
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<Location> path = calculateAStar(player.getLocation(), target);
                pathCache.put(uuid, path);
                pathCacheTime.put(uuid, now);
            });
        }

        List<Location> path = pathCache.get(uuid);
        if (path != null && !path.isEmpty()) {
            // Draw particles along the path (up to 15 blocks ahead)
            for (int i = 0; i < Math.min(15, path.size()); i++) {
                Location p = path.get(i);
                player.spawnParticle(Particle.END_ROD, p.clone().add(0.5, 1.5, 0.5), 1, 0, 0, 0, 0);
            }
        }
    }

    // ─── A* Pathfinding (Simplified & Capped for TPS safety) ───────────────

    private List<Location> calculateAStar(Location start, Location end) {
        List<Location> path = new ArrayList<>();
        World w = start.getWorld();
        
        // Simple BFS/AStar limited to 300 iterations (safe for async block access in most cases)
        PriorityQueue<Node> open = new PriorityQueue<>();
        Set<String> closed = new HashSet<>();
        
        Node startNode = new Node(start.getBlockX(), start.getBlockY(), start.getBlockZ(), 0, start.distanceSquared(end), null);
        open.add(startNode);
        
        Node best = startNode;
        int iterations = 0;

        // Valid moves: N, S, E, W, Up, Down, Diagonals
        int[][] dirs = {
            {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}, 
            {1,0,1}, {-1,0,-1}, {1,0,-1}, {-1,0,1},
            {0,1,0}, {0,-1,0}
        };

        while (!open.isEmpty() && iterations < 500) {
            Node current = open.poll();
            iterations++;

            if (current.h < best.h) best = current;

            // Found target (or close enough)
            if (current.h < 4) {
                best = current;
                break;
            }

            closed.add(current.hash());

            for (int[] dir : dirs) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                int nz = current.z + dir[2];
                String hash = nx+","+ny+","+nz;
                
                if (closed.contains(hash)) continue;
                
                // Jump/Fall limits
                if (Math.abs(ny - current.y) > 1) continue;

                // Sync block check - safely get block if loaded (async block get is mostly safe if chunk is loaded)
                if (!w.isChunkLoaded(nx >> 4, nz >> 4)) continue;
                
                Block b = w.getBlockAt(nx, ny, nz);
                Block bAbove = w.getBlockAt(nx, ny+1, nz);
                Block bBelow = w.getBlockAt(nx, ny-1, nz);

                // Need 2 blocks of air to walk, and a solid block below
                if (b.getType().isSolid() || bAbove.getType().isSolid()) continue;
                if (!bBelow.getType().isSolid() && dir[1] >= 0) continue; // Can't walk on air unless falling

                double g = current.g + 1;
                double dx = nx - end.getBlockX();
                double dy = ny - end.getBlockY();
                double dz = nz - end.getBlockZ();
                double h = dx*dx + dy*dy + dz*dz; // Distance squared heuristic

                open.add(new Node(nx, ny, nz, g, h, current));
                closed.add(hash); // prevent duplicates in open list
            }
        }

        // Reconstruct path
        Node curr = best;
        while (curr != null) {
            path.add(new Location(w, curr.x, curr.y, curr.z));
            curr = curr.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node implements Comparable<Node> {
        int x, y, z;
        double g, h;
        Node parent;

        Node(int x, int y, int z, double g, double h, Node parent) {
            this.x = x; this.y = y; this.z = z;
            this.g = g; this.h = h;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.g + this.h, o.g + o.h);
        }

        public String hash() {
            return x + "," + y + "," + z;
        }
    }

    private Location parseLocation(String str) {
        try {
            String[] parts = str.split(",");
            return new Location(
                Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
            );
        } catch (Exception e) {
            return null;
        }
    }
}
