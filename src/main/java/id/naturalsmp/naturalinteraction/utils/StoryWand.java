package id.naturalsmp.naturalinteraction.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class StoryWand {

    public static ItemStack getWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.toComponent("<gradient:#4facfe:#00f2fe><b>Story Creator Wand</b></gradient>"));
            meta.lore(Collections.singletonList(ChatUtils.toComponent("<gray>Right-click a block or NPC to select!")));
            meta.addEnchant(Enchantment.BREACH, 1, true); // Visual effect
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD)
            return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatUtils.serialize(meta.displayName()).contains("Story Creator Wand");
    }
}
