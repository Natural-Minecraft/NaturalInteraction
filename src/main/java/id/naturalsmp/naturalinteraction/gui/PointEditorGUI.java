package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.cinematic.CameraPoint;
import id.naturalsmp.naturalinteraction.cinematic.CinematicSequence;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PointEditorGUI extends GUI {

    private final CinematicSequence sequence;
    private int durationTicks = 60;
    private CameraPoint.EasingType easing = CameraPoint.EasingType.SMOOTH;
    private String subtitleText = "";

    public PointEditorGUI(NaturalInteraction plugin, Player player, CinematicSequence sequence) {
        super(plugin, player, 27, "&8Pengaturan Titik Kamera");
        this.sequence = sequence;
    }

    @Override
    public void initialize() {
        inventory.clear();

        // Duration Button
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta dm = durationItem.getItemMeta();
        dm.displayName(ChatUtils.toComponent("&eDurasi Transisi"));
        dm.lore(List.of(
                ChatUtils.toComponent("&7Kecepatan kamera menuju ke titik ini."),
                ChatUtils.toComponent("&fSekarang: &b" + durationTicks + " tick &8(" + (durationTicks / 20.0) + " detik)"),
                Component.empty(),
                ChatUtils.toComponent("&eKlik Kiri &8= &a+10 tick"),
                ChatUtils.toComponent("&6Shift Klik Kiri &8= &a+20 tick"),
                ChatUtils.toComponent("&cKlik Kanan &8= &c-10 tick")
        ));
        durationItem.setItemMeta(dm);
        inventory.setItem(11, durationItem);

        // Easing Button
        ItemStack easingItem = new ItemStack(Material.FEATHER);
        ItemMeta em = easingItem.getItemMeta();
        em.displayName(ChatUtils.toComponent("&eGaya Animasi (Easing)"));
        em.lore(List.of(
                ChatUtils.toComponent("&7Gaya kelengkungan kecepatan (easing)."),
                ChatUtils.toComponent("&fTerpilih: &b" + easing.name()),
                Component.empty(),
                ChatUtils.toComponent("&eKlik &7untuk mengganti")
        ));
        easingItem.setItemMeta(em);
        inventory.setItem(13, easingItem);

        // Text Button (we won't use anvil gui here for simplicity, we will use chat prompt if clicked)
        ItemStack textItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta tm = textItem.getItemMeta();
        tm.displayName(ChatUtils.toComponent("&eSubtitle Dialog"));
        String previewText = subtitleText.isEmpty() ? "&8(kosong)" : "&f" + subtitleText;
        tm.lore(List.of(
                ChatUtils.toComponent("&7Teks yang muncul di layar saat titik ini."),
                ChatUtils.toComponent("&7Teks: " + previewText),
                Component.empty(),
                ChatUtils.toComponent("&eKlik &7untuk mengetik di chat")
        ));
        textItem.setItemMeta(tm);
        inventory.setItem(15, textItem);

        // Save & Add
        ItemStack saveItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sm = saveItem.getItemMeta();
        sm.displayName(ChatUtils.toComponent("&a✔ Simpan Titik Ini"));
        sm.lore(List.of(ChatUtils.toComponent("&7Masukkan titik ini ke cinematic.")));
        saveItem.setItemMeta(sm);
        inventory.setItem(26, saveItem);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 11) { // Duration
            if (event.getClick() == ClickType.LEFT) durationTicks += 10;
            else if (event.getClick() == ClickType.SHIFT_LEFT) durationTicks += 20;
            else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) durationTicks = Math.max(0, durationTicks - 10);
            initialize();
        } 
        else if (slot == 13) { // Easing
            CameraPoint.EasingType[] values = CameraPoint.EasingType.values();
            easing = values[(easing.ordinal() + 1) % values.length];
            initialize();
        }
        else if (slot == 15) { // Text
            player.closeInventory();
            player.sendMessage(ChatUtils.toComponent("&aMasukkan subtitle di chat: &7(ketik '-' untuk mengosongkan)"));
            plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                    if (e.getPlayer().equals(player)) {
                        e.setCancelled(true);
                        org.bukkit.event.HandlerList.unregisterAll(this);
                        String msg = e.getMessage().trim();
                        if (msg.equals("-")) subtitleText = "";
                        else subtitleText = msg;
                        Bukkit.getScheduler().runTask(plugin, () -> open());
                    }
                }
            }, plugin);
        }
        else if (slot == 26) { // Save
            sequence.addPoint(new CameraPoint(
                    player.getLocation(), player.getLocation().getYaw(), player.getLocation().getPitch(),
                    durationTicks, easing, subtitleText
            ));
            player.sendMessage(ChatUtils.toComponent("&a✔ Titik kamera ke-" + sequence.getPoints().size() + " disimpan."));
            player.closeInventory();
        }
    }
}
