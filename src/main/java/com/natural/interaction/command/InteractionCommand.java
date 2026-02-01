package com.natural.interaction.command;

import com.natural.interaction.NaturalInteraction;
import com.natural.interaction.hook.InteractionTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InteractionCommand implements CommandExecutor {

    private final NaturalInteraction plugin;

    public InteractionCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        
        if (sub.equals("help")) {
            sendHelp(player);
            return true;
        }
        
        if (sub.equals("create")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /interaction create <name>");
                return true;
            }
            String name = args[1];
            if (plugin.getInteractionManager().hasInteraction(name)) {
                player.sendMessage(ChatColor.RED + "Interaction '" + name + "' already exists.");
                return true;
            }
            plugin.getInteractionManager().createInteraction(name);
            player.sendMessage(ChatColor.GREEN + "Created interaction '" + name + "'. Opening editor...");
            new com.natural.interaction.gui.InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(name)).open();
            return true;
        }
        
        if (sub.equals("play")) {
            if (args.length < 2) {
                // If targeting NPC with interaction?
                NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
                if (npc != null && npc.hasTrait(InteractionTrait.class)) {
                    String id = npc.getTrait(InteractionTrait.class).getInteractionId();
                    if (id != null) {
                        plugin.getInteractionManager().startInteraction(player, id);
                        return true;
                    }
                }
                player.sendMessage(ChatColor.RED + "Usage: /interaction play <name>");
                return true;
            }
            String name = args[1];
            plugin.getInteractionManager().startInteraction(player, name);
            return true;
        }
        
        if (sub.equals("bind")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /interaction bind <name>");
                return true;
            }
            String name = args[1];
             NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
             if (npc == null) {
                 player.sendMessage(ChatColor.RED + "You must have an NPC selected (right click with stick or /npc select).");
                 return true;
             }
             if (!npc.hasTrait(InteractionTrait.class)) {
                 npc.addTrait(InteractionTrait.class);
             }
             npc.getTrait(InteractionTrait.class).setInteractionId(name);
             player.sendMessage(ChatColor.GREEN + "Bound interaction '" + name + "' to NPC " + npc.getName());
             return true;
        }

        if (sub.equals("edit")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /interaction edit <name>");
                return true;
            }
            String name = args[1];
            if (!plugin.getInteractionManager().hasInteraction(name)) {
                player.sendMessage(ChatColor.RED + "Interaction '" + name + "' does not exist.");
                return true;
            }
            new com.natural.interaction.gui.InteractionEditorGUI(plugin, player, plugin.getInteractionManager().getInteraction(name)).open();
            return true;
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("=== Bantuan Fitur Interaksi Interaction ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/interaction create <nama>", NamedTextColor.YELLOW).append(Component.text(" - Membuat interaksi baru dan membuka editor.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/interaction edit <nama>", NamedTextColor.YELLOW).append(Component.text(" - Mengedit interaksi yang sudah ada.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/interaction play <nama>", NamedTextColor.YELLOW).append(Component.text(" - Mencoba memainkan interaksi.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/interaction bind <nama>", NamedTextColor.YELLOW).append(Component.text(" - Menempelkan interaksi ke NPC yang dipilih.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/interaction delete <nama>", NamedTextColor.YELLOW).append(Component.text(" - Menghapus interaksi.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("Fitur Editor:", NamedTextColor.AQUA));
        player.sendMessage(Component.text(" - Dialogue Tree: Percabangan dialog yang kompleks.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Actions: Teleport, Command, Effect, Sound, Zoom.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Rewards: Item hadiah dan Cooldown.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));
    }
}
