package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

public class ActionExecutor {

    public static String execute(Player player, Action action, id.naturalsmp.naturalinteraction.NaturalInteraction plugin) {
        if (action == null || action.getType() == null)
            return null;

        String value = action.getValue();
        if (value == null)
            value = "";

        try {
            switch (action.getType()) {
                case TELEPORT:
                    String[] parts = value.split(",");
                    if (parts.length >= 4) {
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        double z = Double.parseDouble(parts[2]);
                        String world = parts[3];
                        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : player.getLocation().getYaw();
                        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : player.getLocation().getPitch();
                        if (Bukkit.getWorld(world) != null) {
                            player.teleport(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
                        }
                    }
                    break;
                case COMMAND:
                    String cmd = value.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    break;
                case EFFECT:
                    String[] effParts = value.split(",");
                    if (effParts.length >= 2) {
                        PotionEffectType type = PotionEffectType.getByName(effParts[0].toUpperCase());
                        int duration = Integer.parseInt(effParts[1]) * 20;
                        int amplifier = effParts.length > 2 ? Integer.parseInt(effParts[2]) : 0;
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(type, duration, amplifier));
                        }
                    }
                    break;
                case SOUND:
                    String[] soundParts = value.split(",");
                    if (soundParts.length >= 1) {
                        Sound sound = Sound.valueOf(soundParts[0].toUpperCase());
                        float vol = soundParts.length > 1 ? Float.parseFloat(soundParts[1]) : 1.0f;
                        float pitchVal = soundParts.length > 2 ? Float.parseFloat(soundParts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, vol, pitchVal);
                    }
                    break;
                case TITLE:
                    String[] titleParts = value.split(";");
                    String titleText = titleParts[0].replace("%player%", player.getName());
                    String subtitleText = titleParts.length > 1 ? titleParts[1].replace("%player%", player.getName()) : "";

                    Title title = Title.title(
                            ChatUtils.toComponent(titleText),
                            ChatUtils.toComponent(subtitleText),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000),
                                    Duration.ofMillis(1000)));
                    player.showTitle(title);
                    break;
                case MESSAGE:
                    player.sendMessage(ChatUtils.toComponent(value.replace("%player%", player.getName())));
                    break;
                case ZOOM:
                    if ("true".equalsIgnoreCase(value)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 0, false, false));
                    } else {
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                    break;
                case ITEM:
                    String[] itemParts = value.split(",");
                    if (itemParts.length >= 1) {
                        int amount = itemParts.length > 1 ? Integer.parseInt(itemParts[1]) : 1;
                        org.bukkit.inventory.ItemStack itemToGive = null;

                        // Format check for ItemsAdder
                        if (itemParts[0].contains(":")) {
                            dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.getInstance(itemParts[0]);
                            if (customStack != null) {
                                itemToGive = customStack.getItemStack();
                                itemToGive.setAmount(amount);
                            }
                        } else {
                            // Vanilla fallback
                            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(itemParts[0].toUpperCase());
                            if (mat != null) {
                                itemToGive = new org.bukkit.inventory.ItemStack(mat, amount);
                                if (itemParts.length > 2) {
                                    org.bukkit.inventory.meta.ItemMeta meta = itemToGive.getItemMeta();
                                    if (meta != null) {
                                        meta.displayName(id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(itemParts[2]));
                                        if (mat == org.bukkit.Material.FILLED_MAP && meta instanceof org.bukkit.inventory.meta.MapMeta) {
                                            org.bukkit.inventory.meta.MapMeta mapMeta = (org.bukkit.inventory.meta.MapMeta) meta;
                                            org.bukkit.map.MapView view = org.bukkit.Bukkit.createMap(player.getWorld());
                                            mapMeta.setMapView(view);
                                        }
                                        itemToGive.setItemMeta(meta);
                                    }
                                }
                            }
                        }

                        if (itemToGive != null) {
                            id.naturalsmp.naturalinteraction.manager.InteractionSession isession = plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
                            if (isession != null && isession.getOriginalInventory() != null) {
                                isession.addOriginalItem(itemToGive);
                            } else {
                                player.getInventory().addItem(itemToGive);
                            }
                        }
                    }
                    break;
                case ACTIONBAR:
                    player.sendActionBar(id.naturalsmp.naturalinteraction.utils.ChatUtils
                            .toComponent(value.replace("%player%", player.getName())));
                    break;
                case SCREENEFFECT:
                    // value: "effect color fadein stay fadeout freeze|nofreeze"
                    // Dispatches: /screeneffect <value> <player>
                    String screenCmd = "screeneffect " + value.replace("%player%", player.getName());
                    if (!screenCmd.contains(player.getName())) {
                        screenCmd += " " + player.getName();
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), screenCmd);
                    break;
                case INVISIBLE:
                    if ("true".equalsIgnoreCase(value)) {
                        // Make player invisible
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
                        // Hide armor visually by storing and clearing
                        player.setInvisible(true);
                    } else {
                        // Remove invisibility
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        player.setInvisible(false);
                    }
                    break;
                case ADD_TAG:
                    if (plugin != null) {
                        plugin.getInteractionManager().getTagTracker().addTag(player.getUniqueId(), value);
                    }
                    break;
                case REMOVE_TAG:
                    if (plugin != null) {
                        plugin.getInteractionManager().getTagTracker().removeTag(player.getUniqueId(), value);
                    }
                    break;
                case JUMP_IF_TAG:
                    // value: "tag_name,target_node_id"
                    if (plugin != null) {
                        String[] partsJump = value.split(",");
                        if (partsJump.length == 2) {
                            if (plugin.getInteractionManager().getTagTracker().hasTag(player.getUniqueId(), partsJump[0])) {
                                return partsJump[1];
                            }
                        }
                    }
                    break;
                case JUMP_IF_NOT_TAG:
                    // value: "tag_name,target_node_id"
                    if (plugin != null) {
                        String[] partsJumpN = value.split(",");
                        if (partsJumpN.length == 2) {
                            if (!plugin.getInteractionManager().getTagTracker().hasTag(player.getUniqueId(), partsJumpN[0])) {
                                return partsJumpN[1];
                            }
                        }
                    }
                    break;
                case TAKE_ITEM:
                    // value: "material|ia_id,amount"
                    String[] takeParts = value.split(",");
                    if (takeParts.length >= 2) {
                        String itemStr = takeParts[0];
                        int amount = Integer.parseInt(takeParts[1]);
                        takePlayerItem(player, itemStr, amount, plugin);
                    }
                    break;
                case JUMP_IF_ITEM:
                    // value: "material|ia_id,amount,target_node_id"
                    String[] jiParts = value.split(",");
                    if (jiParts.length == 3) {
                        String itemStr = jiParts[0];
                        int amount = Integer.parseInt(jiParts[1]);
                        if (hasPlayerItem(player, itemStr, amount, plugin)) {
                            return jiParts[2];
                        }
                    }
                    break;
                case JUMP_IF_NOT_ITEM:
                    // value: "material|ia_id,amount,target_node_id"
                    String[] jinParts = value.split(",");
                    if (jinParts.length == 3) {
                        String itemStr = jinParts[0];
                        int amount = Integer.parseInt(jinParts[1]);
                        if (!hasPlayerItem(player, itemStr, amount, plugin)) {
                            return jinParts[2];
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to execute action " + action.getType() + ": " + e.getMessage());
        }
        return null;
    }

    private static boolean hasPlayerItem(Player player, String itemString, int amount, id.naturalsmp.naturalinteraction.NaturalInteraction plugin) {
        id.naturalsmp.naturalinteraction.manager.InteractionSession session = 
            plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
            
        if (session != null && session.getOriginalInventory() != null) {
            return session.hasOriginalItem(itemString, amount);
        }

        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            if (itemString.contains(":")) {
                dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.byItemStack(item);
                if (customStack != null && customStack.getNamespacedID().equalsIgnoreCase(itemString)) {
                    count += item.getAmount();
                }
            } else {
                if (item.getType().name().equalsIgnoreCase(itemString)) {
                    count += item.getAmount();
                }
            }
        }
        return count >= amount;
    }

    private static void takePlayerItem(Player player, String itemString, int amount, id.naturalsmp.naturalinteraction.NaturalInteraction plugin) {
        id.naturalsmp.naturalinteraction.manager.InteractionSession session = 
            plugin != null ? plugin.getInteractionManager().getSession(player.getUniqueId()) : null;
            
        if (session != null && session.getOriginalInventory() != null) {
            session.takeOriginalItem(itemString, amount);
            return;
        }

        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;

            boolean match = false;
            if (itemString.contains(":")) {
                dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.byItemStack(item);
                if (customStack != null && customStack.getNamespacedID().equalsIgnoreCase(itemString)) {
                    match = true;
                }
            } else {
                if (item.getType().name().equalsIgnoreCase(itemString)) {
                    match = true;
                }
            }

            if (match) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
        player.updateInventory();
    }
}
