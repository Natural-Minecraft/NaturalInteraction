package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.cinematic.CameraPoint;
import id.naturalsmp.naturalinteraction.cinematic.CinematicSequence;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PointListGUI extends GUI {

    private final CinematicSequence sequence;

    public PointListGUI(NaturalInteraction plugin, Player player, CinematicSequence sequence) {
        super(plugin, player, 54, "&8Daftar Titik Kamera");
        this.sequence = sequence;
    }

    @Override
    public void initialize() {
        inventory.clear();

        List<CameraPoint> points = sequence.getPoints();
        for (int i = 0; i < points.size(); i++) {
            if (i >= 53) break; // Max slots

            CameraPoint pt = points.get(i);
            ItemStack item = new ItemStack(Material.ENDER_EYE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ChatUtils.toComponent("&aTitik #" + (i + 1)));
            meta.lore(List.of(
                    ChatUtils.toComponent("&7Posisi: &f" + String.format("%.1f, %.1f, %.1f", pt.getLocation().getX(), pt.getLocation().getY(), pt.getLocation().getZ())),
                    ChatUtils.toComponent("&7Durasi: &f" + pt.getDurationTicks() + " tick"),
                    ChatUtils.toComponent("&7Easing: &f" + pt.getEasing().name()),
                    ChatUtils.toComponent(""),
                    ChatUtils.toComponent("&eKlik untuk mengedit titik ini.")
            ));
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Add new point button
        ItemStack addBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta addMeta = addBtn.getItemMeta();
        addMeta.displayName(ChatUtils.toComponent("&a+ Tambah Titik Baru"));
        addMeta.lore(List.of(ChatUtils.toComponent("&7Buat titik kamera di lokasimu saat ini.")));
        addBtn.setItemMeta(addMeta);
        inventory.setItem(53, addBtn);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 53) {
            new PointEditorGUI(plugin, player, sequence, -1).open();
            return;
        }

        if (slot >= 0 && slot < sequence.getPoints().size()) {
            new PointEditorGUI(plugin, player, sequence, slot).open();
        }
    }
}
