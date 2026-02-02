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

    public static void execute(Player player, Action action) {
        if (action == null || action.getType() == null)
            return;

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
                    String titleText = titleParts[0];
                    String subtitleText = titleParts.length > 1 ? titleParts[1] : "";

                    Title title = Title.title(
                            Component.text(titleText),
                            Component.text(subtitleText),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000),
                                    Duration.ofMillis(1000)));
                    player.showTitle(title);
                    break;
                case MESSAGE:
                    player.sendMessage(Component.text(value.replace("%player%", player.getName())));
                    break;
                case ZOOM:
                    if ("true".equalsIgnoreCase(value)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 99999, 4, false, false));
                    } else {
                        player.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                    break;
                case ITEM:
                    String[] itemParts = value.split(",");
                    if (itemParts.length >= 1) {
                        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(itemParts[0].toUpperCase());
                        int amount = itemParts.length > 1 ? Integer.parseInt(itemParts[1]) : 1;
                        if (mat != null) {
                            org.bukkit.inventory.ItemStack is = new org.bukkit.inventory.ItemStack(mat, amount);
                            if (itemParts.length > 2) {
                                org.bukkit.inventory.meta.ItemMeta meta = is.getItemMeta();
                                if (meta != null) {
                                    meta.displayName(
                                            id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(itemParts[2]));
                                    is.setItemMeta(meta);
                                }
                            }
                            player.getInventory().addItem(is);
                        }
                    }
                    break;
                case ACTIONBAR:
                    player.sendActionBar(id.naturalsmp.naturalinteraction.utils.ChatUtils
                            .toComponent(value.replace("%player%", player.getName())));
                    break;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to execute action " + action.getType() + ": " + e.getMessage());
        }
    }
}
