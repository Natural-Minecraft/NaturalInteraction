package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.TagTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SidequestCommand implements CommandExecutor {

    private final NaturalInteraction plugin;

    public SidequestCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Hanya player yang dapat mengeksekusi command ini.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        TagTracker tracker = plugin.getInteractionManager().getTagTracker();

        player.sendMessage(Component.text("--- Daftar Sidequest Kamu ---", NamedTextColor.GOLD));

        int questCount = 0;

        // Quest 1: Coffeeman
        if (tracker.hasTag(uuid, "coffeeman_quest_started") && !tracker.hasTag(uuid, "coffeeman_quest_done")) {
            player.sendMessage(Component.text("☕ The Coffeeman", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - Bawakan 3 Cocoa Beans dan 1 Empty Cup.", NamedTextColor.GRAY));
            questCount++;
        }

        // Quest 2: npcCosmetics (Secondary Quest)
        if (tracker.hasTag(uuid, "cosmetics_coffee_quest_started") && !tracker.hasTag(uuid, "cosmetics_coffee_quest_done")) {
            player.sendMessage(Component.text("💄 Kopi untuk si Penasaran", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - Buatkan secangkir Kopi menggunakan Coffee Machine dan berikan padanya.", NamedTextColor.GRAY));
            questCount++;
        }

        if (questCount == 0) {
            player.sendMessage(Component.text("Kamu belum memiliki sidequest aktif saat ini.", NamedTextColor.GRAY));
        }

        player.sendMessage(Component.text("-----------------------------", NamedTextColor.GOLD));
        return true;
    }
}
