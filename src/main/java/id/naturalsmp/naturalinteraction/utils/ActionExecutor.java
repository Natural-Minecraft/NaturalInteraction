package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.facts.FactsManager;
import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.ActionType;
import id.naturalsmp.naturalinteraction.session.InventorySnapshot;
import id.naturalsmp.naturalinteraction.manager.InteractionSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

/**
 * Executes a single {@link Action} for a player during a dialogue node.
 *
 * Returns a node ID (String) if the action causes a node jump (JUMP_IF_*),
 * or null to continue normal flow.
 *
 * Facts actions (SET_FACT, ADD_FACT, JUMP_IF_FACT, etc.) use the new
 * unified {@link FactsManager}. Legacy tag actions (ADD_TAG, REMOVE_TAG,
 * JUMP_IF_TAG, JUMP_IF_NOT_TAG) are mapped to Facts equivalents.
 */
public class ActionExecutor {

    public static String execute(Player player, Action action, NaturalInteraction plugin) {
        if (action == null || action.getType() == null) return null;

        String value = action.getValue() != null ? action.getValue() : "";
        FactsManager facts = plugin != null ? plugin.getFactsManager() : null;

        try {
            switch (action.getType()) {

                // ─── Movement & World ─────────────────────────────────────────
                case TELEPORT -> {
                    String[] p = value.split(",");
                    if (p.length >= 4 && Bukkit.getWorld(p[3]) != null) {
                        double x = Double.parseDouble(p[0]);
                        double y = Double.parseDouble(p[1]);
                        double z = Double.parseDouble(p[2]);
                        float yaw   = p.length > 4 ? Float.parseFloat(p[4]) : player.getLocation().getYaw();
                        float pitch = p.length > 5 ? Float.parseFloat(p[5]) : player.getLocation().getPitch();
                        player.teleport(new Location(Bukkit.getWorld(p[3]), x, y, z, yaw, pitch));
                    }
                }
                case COMMAND -> {
                    String cmd = value.replace("%player%", player.getName())
                                     .replace("%player_name%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                case EFFECT -> {
                    String[] p = value.split(",");
                    if (p.length >= 2) {
                        PotionEffectType type = PotionEffectType.getByName(p[0].toUpperCase());
                        int duration = Integer.parseInt(p[1]) * 20;
                        int amp = p.length > 2 ? Integer.parseInt(p[2]) : 0;
                        if (type != null) player.addPotionEffect(new PotionEffect(type, duration, amp));
                    }
                }
                case SOUND -> {
                    String[] p = value.split(",");
                    Sound sound = Sound.valueOf(p[0].toUpperCase());
                    float vol   = p.length > 1 ? Float.parseFloat(p[1]) : 1.0f;
                    float pitch = p.length > 2 ? Float.parseFloat(p[2]) : 1.0f;
                    player.playSound(player.getLocation(), sound, vol, pitch);
                }
                case TITLE -> {
                    String[] p = value.split(";", 2);
                    String t = p[0].replace("%player%", player.getName());
                    String s = p.length > 1 ? p[1].replace("%player%", player.getName()) : "";
                    player.showTitle(Title.title(
                            ChatUtils.toComponent(t), ChatUtils.toComponent(s),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
                }
                case MESSAGE  -> player.sendMessage(ChatUtils.toComponent(value.replace("%player%", player.getName())));
                case ACTIONBAR -> player.sendActionBar(ChatUtils.toComponent(value.replace("%player%", player.getName())));
                case SCREENEFFECT -> {
                    String cmd = "screeneffect " + value.replace("%player%", player.getName());
                    if (!cmd.contains(player.getName())) cmd += " " + player.getName();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                case ZOOM -> {
                    if ("true".equalsIgnoreCase(value)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 0, false, false));
                    } else {
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                }
                case INVISIBLE -> {
                    if ("true".equalsIgnoreCase(value)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                        player.setInvisible(true);
                    } else {
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        player.setInvisible(false);
                    }
                }
                case CINEMATIC -> {
                    if (plugin != null && plugin.getCinematicManager() != null) {
                        id.naturalsmp.naturalinteraction.cinematic.CinematicSequence seq = plugin.getCinematicManager().getSequence(value);
                        if (seq != null) {
                            plugin.getCinematicManager().getPlayer().play(player, seq);
                        } else {
                            Bukkit.getLogger().warning("[NI] Cinematic sequence not found: " + value);
                        }
                    }
                }

                // ─── Items ────────────────────────────────────────────────────
                case ITEM -> {
                    String[] p = value.split(",");
                    int amount = p.length > 1 ? Integer.parseInt(p[1]) : 1;
                    ItemStack item = resolveItem(p[0], amount, p.length > 2 ? p[2] : null);
                    if (item != null) giveItem(player, item, plugin);
                }
                case TAKE_ITEM -> {
                    String[] p = value.split(",");
                    if (p.length >= 2) takeItem(player, p[0], Integer.parseInt(p[1]), plugin);
                }
                case JUMP_IF_ITEM -> {
                    String[] p = value.split(",");
                    if (p.length == 3 && hasItem(player, p[0], Integer.parseInt(p[1]), plugin)) return p[2];
                }
                case JUMP_IF_NOT_ITEM -> {
                    String[] p = value.split(",");
                    if (p.length == 3 && !hasItem(player, p[0], Integer.parseInt(p[1]), plugin)) return p[2];
                }

                // ─── Facts ────────────────────────────────────────────────────
                case SET_FACT -> {
                    // value: "factKey,value"
                    if (facts == null) break;
                    int idx = value.indexOf(',');
                    if (idx > 0) facts.setString(player.getUniqueId(), value.substring(0, idx), value.substring(idx + 1));
                }
                case ADD_FACT -> {
                    // value: "factKey,delta"
                    if (facts == null) break;
                    String[] p = value.split(",", 2);
                    if (p.length == 2) {
                        try { facts.addInt(player.getUniqueId(), p[0], Integer.parseInt(p[1])); }
                        catch (NumberFormatException e) {
                            try { facts.setFloat(player.getUniqueId(), p[0],
                                    facts.getFloat(player.getUniqueId(), p[0], 0) + Float.parseFloat(p[1])); }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                }
                case REMOVE_FACT -> {
                    if (facts != null) facts.remove(player.getUniqueId(), value);
                }
                case JUMP_IF_FACT -> {
                    // value: "factKey,expectedValue,targetNodeId"
                    if (facts == null) break;
                    String[] p = value.split(",", 3);
                    if (p.length == 3) {
                        String actual = facts.getString(player.getUniqueId(), p[0], "");
                        if (p[1].equalsIgnoreCase(actual)) return p[2];
                    }
                }
                case JUMP_IF_NOT_FACT -> {
                    // value: "factKey,expectedValue,targetNodeId"
                    if (facts == null) break;
                    String[] p = value.split(",", 3);
                    if (p.length == 3) {
                        String actual = facts.getString(player.getUniqueId(), p[0], "");
                        if (!p[1].equalsIgnoreCase(actual)) return p[2];
                    }
                }
                case JUMP_IF_FACT_GT -> {
                    // value: "factKey,threshold,targetNodeId"
                    if (facts == null) break;
                    String[] p = value.split(",", 3);
                    if (p.length == 3) {
                        float actual = facts.getFloat(player.getUniqueId(), p[0], 0);
                        if (actual > Float.parseFloat(p[1])) return p[2];
                    }
                }
                case JUMP_IF_FACT_LT -> {
                    // value: "factKey,threshold,targetNodeId"
                    if (facts == null) break;
                    String[] p = value.split(",", 3);
                    if (p.length == 3) {
                        float actual = facts.getFloat(player.getUniqueId(), p[0], 0);
                        if (actual < Float.parseFloat(p[1])) return p[2];
                    }
                }

                // ─── Legacy Tag (maps to Facts) ───────────────────────────────
                case ADD_TAG -> {
                    if (facts != null) facts.addTag(player.getUniqueId(), value);
                }
                case REMOVE_TAG -> {
                    if (facts != null) facts.removeTag(player.getUniqueId(), value);
                }
                case JUMP_IF_TAG -> {
                    if (facts == null) break;
                    String[] p = value.split(",", 2);
                    if (p.length == 2 && facts.hasTag(player.getUniqueId(), p[0])) return p[1];
                }
                case JUMP_IF_NOT_TAG -> {
                    if (facts == null) break;
                    String[] p = value.split(",", 2);
                    if (p.length == 2 && !facts.hasTag(player.getUniqueId(), p[0])) return p[1];
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NI] Action " + action.getType() + " failed: " + e.getMessage());
        }
        return null;
    }

    // ─── Item Helpers ─────────────────────────────────────────────────────────

    private static ItemStack resolveItem(String id, int amount, String displayName) {
        if (id.contains(":")) {
            try {
                dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.getInstance(id);
                if (cs != null) {
                    ItemStack item = cs.getItemStack();
                    item.setAmount(amount);
                    return item;
                }
            } catch (NoClassDefFoundError ignored) {}
        }
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(id.toUpperCase());
        if (mat == null) return null;
        ItemStack item = new ItemStack(mat, amount);
        if (displayName != null && item.hasItemMeta()) {
            var meta = item.getItemMeta();
            meta.displayName(ChatUtils.toComponent(displayName));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void giveItem(Player player, ItemStack item, NaturalInteraction plugin) {
        InteractionSession session = plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
        if (session != null) {
            session.addOriginalItem(item);
        } else {
            player.getInventory().addItem(item);
        }
    }

    private static boolean hasItem(Player player, String itemString, int amount, NaturalInteraction plugin) {
        InteractionSession session = plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
        if (session != null) return session.hasOriginalItem(itemString, amount);

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (matchesItem(item, itemString)) count += item.getAmount();
        }
        return count >= amount;
    }

    private static void takeItem(Player player, String itemString, int amount, NaturalInteraction plugin) {
        InteractionSession session = plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
        if (session != null) { session.takeOriginalItem(itemString, amount); return; }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (!matchesItem(item, itemString)) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) break;
        }
        player.updateInventory();
    }

    private static boolean matchesItem(ItemStack item, String id) {
        if (id.contains(":")) {
            try {
                dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.byItemStack(item);
                return cs != null && cs.getNamespacedID().equalsIgnoreCase(id);
            } catch (NoClassDefFoundError ignored) { return false; }
        }
        return item.getType().name().equalsIgnoreCase(id);
    }
}
