package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.facts.FactsManager;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Root command for NaturalInteraction v2.
 * Usage: /ni <subcommand> [args...]
 *
 * Subcommands:
 *  reload          Reload the plugin
 *  clearChat       Clear player's chat
 *  connect         Get the Web Panel link
 *  facts [player]  View a player's facts
 *  facts set <key> <value> [player]
 *  facts reset [player]
 *  trigger <entry> [player]
 *  fire <entry> [player]
 *  cinematic <start|stop> <page> [player]
 *  quest track <quest> [player]
 *  untrack [player]
 *  manifest inspect [player]
 *  assets clean
 */
public class NiCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "naturalsmp.admin";
    private static final String PERM_USE   = "naturalsmp.ni";

    private final NaturalInteraction plugin;

    public NiCommand(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload"    -> handleReload(sender);
            case "clearchat" -> handleClearChat(sender, args);
            case "connect"   -> handleConnect(sender);
            case "facts"     -> handleFacts(sender, args);
            case "trigger"   -> handleTrigger(sender, args);
            case "fire"      -> handleFire(sender, args);
            case "cinematic" -> handleCinematic(sender, args);
            case "quest"     -> handleQuest(sender, args);
            case "untrack"   -> handleUntrack(sender, args);
            case "manifest"  -> handleManifest(sender, args);
            case "assets"    -> handleAssets(sender, args);
            default          -> sendHelp(sender);
        }
        return true;
    }

    // ─── /ni reload ───────────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        plugin.reloadPlugin();
        sender.sendMessage(ChatUtils.toComponent("&a✔ NaturalInteraction reloaded."));
    }

    // ─── /ni clearChat [player] ───────────────────────────────────────────────

    private void handleClearChat(CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return;
        for (int i = 0; i < 100; i++) target.sendMessage(Component.empty());
        target.sendMessage(ChatUtils.toComponent("&8[&7Chat dibersihkan&8]"));
        if (!target.equals(sender))
            sender.sendMessage(ChatUtils.toComponent("&7Chat &e" + target.getName() + " &7dibersihkan."));
    }

    // ─── /ni connect ──────────────────────────────────────────────────────────

    private void handleConnect(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        // TODO: generate one-time token from WebPanelServer when implemented
        String url = "http://localhost:" + plugin.getConfig().getInt("webpanel.port", 8585)
                + "?token=coming-soon";
        sender.sendMessage(ChatUtils.toComponent("&6✦ &eNaturalInteraction Web Panel"));
        sender.sendMessage(Component.text("  ").append(
                Component.text(url, NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(url))));
        sender.sendMessage(ChatUtils.toComponent("  &7(Klik untuk buka di browser)"));
    }

    // ─── /ni facts [player] | facts set <key> <val> [player] | facts reset [player] ──

    private void handleFacts(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }

        // facts reset [player]
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            Player target = resolvePlayer(sender, args, 2);
            if (target == null) return;
            plugin.getFactsManager().resetAll(target.getUniqueId());
            sender.sendMessage(ChatUtils.toComponent("&a✔ All facts reset for &e" + target.getName()));
            return;
        }

        // facts set <key> <value> [player]
        if (args.length >= 4 && args[1].equalsIgnoreCase("set")) {
            Player target = resolvePlayer(sender, args, 4);
            if (target == null) return;
            plugin.getFactsManager().setString(target.getUniqueId(), args[2], args[3]);
            sender.sendMessage(ChatUtils.toComponent("&a✔ Fact &e" + args[2] + " &a= &f" + args[3]
                    + " &adata ke &e" + target.getName()));
            return;
        }

        // facts [player] → view all facts
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return;
        Map<String, String> facts = plugin.getFactsManager().getAll(target.getUniqueId());
        sender.sendMessage(ChatUtils.toComponent("&6--- Facts: &e" + target.getName() + " &6(" + facts.size() + ") ---"));
        if (facts.isEmpty()) {
            sender.sendMessage(ChatUtils.toComponent("  &7(tidak ada facts)"));
        } else {
            facts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sender.sendMessage(
                            ChatUtils.toComponent("  &7" + e.getKey() + " &8= &f" + e.getValue())));
        }
    }

    // ─── /ni trigger <entry> [player] ─────────────────────────────────────────

    private void handleTrigger(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(ChatUtils.toComponent("&cUsage: /ni trigger <entry> [player]")); return; }
        Player target = resolvePlayer(sender, args, 2);
        if (target == null) return;
        String interactionId = args[1];
        if (!plugin.getInteractionManager().hasInteraction(interactionId)) {
            sender.sendMessage(ChatUtils.toComponent("&cInteraction tidak ditemukan: &f" + interactionId));
            return;
        }
        plugin.getInteractionManager().startInteraction(target, interactionId);
        sender.sendMessage(ChatUtils.toComponent("&a✔ Triggered &e" + interactionId + " &afor &e" + target.getName()));
    }

    // ─── /ni fire <entry> [player] ────────────────────────────────────────────

    private void handleFire(CommandSender sender, String[] args) {
        // Alias for trigger — fires a trigger event entry
        handleTrigger(sender, args);
    }

    // ─── /ni cinematic <start|stop> <page> [player] ───────────────────────────

    private void handleCinematic(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        if (args.length < 3) {
            sender.sendMessage(ChatUtils.toComponent("&cUsage: /ni cinematic <start|stop> <page> [player]"));
            return;
        }
        // TODO: implement when CinematicManager is built in Phase 6
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Cinematic system akan tersedia di Phase 6."));
    }

    // ─── /ni quest track <quest> [player] ─────────────────────────────────────

    private void handleQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        if (args.length < 3 || !args[1].equalsIgnoreCase("track")) {
            sender.sendMessage(ChatUtils.toComponent("&cUsage: /ni quest track <questEntry> [player]"));
            return;
        }
        Player target = resolvePlayer(sender, args, 3);
        if (target == null) return;
        // TODO: wire into QuestManager when built
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Quest system dalam pengembangan."));
    }

    // ─── /ni untrack [player] ─────────────────────────────────────────────────

    private void handleUntrack(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return;
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Quest system dalam pengembangan."));
    }

    // ─── /ni manifest inspect [player] ────────────────────────────────────────

    private void handleManifest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        // TODO: wire into ManifestManager in Phase 4
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Manifest system akan tersedia di Phase 4."));
    }

    // ─── /ni assets clean ─────────────────────────────────────────────────────

    private void handleAssets(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("clean")) {
            sender.sendMessage(ChatUtils.toComponent("&cUsage: /ni assets clean"));
            return;
        }
        // TODO: implement asset cleanup when asset system is built
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Asset system dalam pengembangan."));
    }

    // ─── Help ─────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.toComponent("<gradient:#4facfe:#00f2fe><b>NaturalInteraction v2</b></gradient> &8— /ni"));
        List<String[]> cmds = List.of(
                new String[]{"reload", "Reload plugin"},
                new String[]{"clearChat [player]", "Bersihkan chat"},
                new String[]{"connect", "Buka Web Panel"},
                new String[]{"facts [player]", "Lihat facts player"},
                new String[]{"facts set <key> <val> [player]", "Set fact"},
                new String[]{"facts reset [player]", "Reset semua facts"},
                new String[]{"trigger <entry> [player]", "Trigger interaction"},
                new String[]{"cinematic <start|stop> <page> [player]", "Main cinematic"},
                new String[]{"quest track <quest> [player]", "Track quest"},
                new String[]{"untrack [player]", "Stop tracking quest"},
                new String[]{"manifest inspect [player]", "Inspect manifest"},
                new String[]{"assets clean", "Bersihkan asset tidak terpakai"}
        );
        for (String[] cmd : cmds) {
            sender.sendMessage(ChatUtils.toComponent("  &8/ni &7" + cmd[0] + " &8— &f" + cmd[1]));
        }
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) return List.of();

        List<String> subs = List.of("reload", "clearChat", "connect", "facts",
                "trigger", "fire", "cinematic", "quest", "untrack", "manifest", "assets");

        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        switch (args[0].toLowerCase()) {
            case "facts" -> {
                if (args.length == 2) return List.of("set", "reset");
                if (args.length >= 3) return onlinePlayers(args[args.length - 1]);
            }
            case "trigger", "fire" -> {
                if (args.length == 2) return new ArrayList<>(plugin.getInteractionManager().getInteractionIds());
                if (args.length == 3) return onlinePlayers(args[2]);
            }
            case "clearchat", "untrack" -> {
                if (args.length == 2) return onlinePlayers(args[1]);
            }
            case "cinematic" -> {
                if (args.length == 2) return List.of("start", "stop");
                if (args.length == 3) return new ArrayList<>(plugin.getInteractionManager().getInteractionIds());
                if (args.length == 4) return onlinePlayers(args[3]);
            }
            case "quest" -> {
                if (args.length == 2) return List.of("track");
            }
            case "assets" -> {
                if (args.length == 2) return List.of("clean");
            }
        }
        return List.of();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Player resolvePlayer(CommandSender sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player p = Bukkit.getPlayer(args[argIndex]);
            if (p == null) {
                sender.sendMessage(ChatUtils.toComponent("&cPlayer tidak ditemukan: &f" + args[argIndex]));
                return null;
            }
            return p;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage(ChatUtils.toComponent("&cHarus specify player dari console: /ni <cmd> <player>"));
        return null;
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(ChatUtils.toComponent("&cKamu tidak memiliki permission untuk command ini."));
    }
}
