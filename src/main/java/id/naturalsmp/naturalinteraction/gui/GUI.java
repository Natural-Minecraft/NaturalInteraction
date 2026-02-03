package id.naturalsmp.naturalinteraction.gui;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class GUI implements InventoryHolder {
    protected final NaturalInteraction plugin;
    protected Inventory inventory;
    protected final Player player;

    public GUI(NaturalInteraction plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size,
                id.naturalsmp.naturalinteraction.utils.ChatUtils.toComponent(title));
    }

    public abstract void initialize();

    public void open() {
        initialize();
        player.openInventory(inventory);
    }

    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public boolean isInteractable() {
        return false;
    }

    public void handleClose(InventoryCloseEvent event) {
    }
}
