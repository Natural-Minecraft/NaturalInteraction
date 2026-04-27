package id.naturalsmp.naturalinteraction.commands;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.commands.subs.*;
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
 * Unified root command for NaturalInteraction v2.
 * Merges legacy InteractionCommand subcommands (create, edit, bind, etc.)
 * with new v2 subcommands (facts, trigger, cinematic, etc.)
 *
 * Usage: /ni <subcommand> [args...]
 * Aliases: /interaction, /inter
 */
public class NiCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "naturalsmp.admin";

    private final NaturalInteraction plugin;

    // Legacy SubCommand system (create, edit, bind, delete, etc.)
    private final Map<String, SubCommand> legacySubs = new HashMap<>();

    // New v2 subcommand names
    private static final Set<String> V2_SUBS = Set.of(
            "reload", "clearchat", "connect", "facts", "trigger",
            "fire", "cinematic", "quest", "untrack", "manifest", "assets");

    public NiCommand(NaturalInteraction plugin) {
        this.plugin = plugin;

        // Register legacy SubCommands
        registerLegacy(new StartCommand(plugin));
        registerLegacy(new EditorCommand(plugin));
        registerLegacy(new WandCommand(plugin));
        registerLegacy(new CreateCommand(plugin));
        registerLegacy(new PlayCommand(plugin));
        registerLegacy(new BindCommand(plugin));
        registerLegacy(new EditCommand(plugin));
        registerLegacy(new DeleteCommand(plugin));
        registerLegacy(new SpawnFishCommand(plugin));
        registerLegacy(new QuickStartCommand(plugin));
        registerLegacy(new SpawnSmartCommand(plugin));
        // Note: ReloadCommand is replaced by v2 reload below
    }

    private void registerLegacy(SubCommand sub) {
        legacySubs.put(sub.getName().toLowerCase(), sub);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        String sub = args[0].toLowerCase();

        // V2 subcommands take priority
        if (V2_SUBS.contains(sub)) {
            return handleV2(sender, sub, args);
        }

        // Legacy SubCommand fallback (create, edit, bind, etc.)
        SubCommand legacy = legacySubs.get(sub);
        if (legacy != null) {
            if (legacy.getPermission() != null && !sender.hasPermission(legacy.getPermission())) {
                sender.sendMessage(ChatUtils.toComponent("&cKamu tidak memiliki permission."));
                return true;
            }
            legacy.execute(sender, args);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    // ─── V2 Subcommands ───────────────────────────────────────────────────────

    private boolean handleV2(CommandSender sender, String sub, String[] args) {
        switch (sub) {
            case "reload"    -> handleReload(sender);
            case "clearchat" -> handleClearChat(sender, args);
            case "connect"   -> handleConnect(sender);
            case "facts"     -> handleFacts(sender, args);
            case "trigger"   -> handleTrigger(sender, args);
            case "fire"      -> handleTrigger(sender, args); // alias
            case "cinematic" -> handleCinematic(sender, args);
            case "quest"     -> handleQuest(sender, args);
            case "untrack"   -> handleUntrack(sender, args);
            case "manifest"  -> handleManifest(sender, args);
            case "assets"    -> handleAssets(sender, args);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        plugin.reloadPlugin();
        sender.sendMessage(ChatUtils.toComponent("&a✔ NaturalInteraction reloaded."));
    }

    private void handleClearChat(CommandSender sender, String[] args) {
        Player target = resolvePlayer(sender, args, 1);
        if (target == null) return;
        for (int i = 0; i < 100; i++) target.sendMessage(Component.empty());
        target.sendMessage(ChatUtils.toComponent("&8[&7Chat dibersihkan&8]"));
        if (!target.equals(sender))
            sender.sendMessage(ChatUtils.toComponent("&7Chat &e" + target.getName() + " &7dibersihkan."));
    }

    private void handleConnect(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        String url = "http://localhost:" + plugin.getConfig().getInt("webpanel.port", 8585);
        sender.sendMessage(ChatUtils.toComponent("&6✦ &eNaturalInteraction Web Panel"));
        sender.sendMessage(Component.text("  ").append(
                Component.text(url, NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(url))));
    }

    private void handleFacts(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        FactsManager fm = plugin.getFactsManager();

        // facts reset [player]
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            Player t = resolvePlayer(sender, args, 2);
            if (t == null) return;
            fm.resetAll(t.getUniqueId());
            sender.sendMessage(ChatUtils.toComponent("&a✔ Facts reset for &e" + t.getName()));
            return;
        }
        // facts set <key> <value> [player]
        if (args.length >= 4 && args[1].equalsIgnoreCase("set")) {
            Player t = resolvePlayer(sender, args, 4);
            if (t == null) return;
            fm.setString(t.getUniqueId(), args[2], args[3]);
            sender.sendMessage(ChatUtils.toComponent("&a✔ &e" + args[2] + " &a= &f" + args[3]
                    + " &afor &e" + t.getName()));
            return;
        }
        // facts [player] → view
        Player t = resolvePlayer(sender, args, 1);
        if (t == null) return;
        Map<String, String> facts = fm.getAll(t.getUniqueId());
        sender.sendMessage(ChatUtils.toComponent("&6--- Facts: &e" + t.getName()
                + " &6(" + facts.size() + ") ---"));
        if (facts.isEmpty()) {
            sender.sendMessage(ChatUtils.toComponent("  &7(tidak ada facts)"));
        } else {
            facts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sender.sendMessage(
                            ChatUtils.toComponent("  &7" + e.getKey() + " &8= &f" + e.getValue())));
        }
    }

    private void handleTrigger(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage(ChatUtils.toComponent("&c/ni trigger <entry> [player]")); return; }
        Player t = resolvePlayer(sender, args, 2);
        if (t == null) return;
        if (!plugin.getInteractionManager().hasInteraction(args[1])) {
            sender.sendMessage(ChatUtils.toComponent("&cInteraction tidak ditemukan: &f" + args[1]));
            return;
        }
        plugin.getInteractionManager().startInteraction(t, args[1]);
        sender.sendMessage(ChatUtils.toComponent("&a✔ Triggered &e" + args[1] + " &afor &e" + t.getName()));
    }

    private void handleCinematic(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Cinematic system (Phase 6)"));
    }

    private void handleQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Quest tracking system"));
    }

    private void handleUntrack(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Quest tracking system"));
    }

    private void handleManifest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Manifest system (Phase 4)"));
    }

    private void handleAssets(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) { noPermission(sender); return; }
        sender.sendMessage(ChatUtils.toComponent("&eWIP: Asset system"));
    }

    // ─── Help ─────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.toComponent(
                "<gradient:#4facfe:#00f2fe><b>NaturalInteraction v2</b></gradient> &8— /ni"));
        sender.sendMessage(Component.empty());

        // V2 commands
        sender.sendMessage(ChatUtils.toComponent("&6&lV2 Commands:"));
        for (String[] c : List.of(
                new String[]{"reload", "Reload plugin"},
                new String[]{"clearChat [player]", "Bersihkan chat"},
                new String[]{"connect", "Buka Web Panel"},
                new String[]{"facts [player]", "Lihat facts player"},
                new String[]{"facts set <key> <val> [player]", "Set fact"},
                new String[]{"facts reset [player]", "Reset semua facts"},
                new String[]{"trigger <entry> [player]", "Trigger interaction"},
                new String[]{"cinematic <start|stop> <page> [player]", "Main cinematic"},
                new String[]{"quest track <quest> [player]", "Track quest"},
                new String[]{"untrack [player]", "Stop tracking"},
                new String[]{"manifest inspect [player]", "Inspect manifest"}
        )) {
            sender.sendMessage(ChatUtils.toComponent("  &8/ni &7" + c[0] + " &8— &f" + c[1]));
        }

        // Legacy editor commands
        sender.sendMessage(Component.empty());
        sender.sendMessage(ChatUtils.toComponent("&6&lEditor Commands:"));
        legacySubs.values().stream()
                .filter(s -> s.getPermission() == null || sender.hasPermission(s.getPermission()))
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(s -> sender.sendMessage(
                        ChatUtils.toComponent("  &8/ni &7" + s.getUsage() + " &8— &f" + s.getDescription())));
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> all = new ArrayList<>(V2_SUBS);
            legacySubs.keySet().forEach(all::add);
            return all.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // V2 tab completion
        switch (args[0].toLowerCase()) {
            case "facts" -> {
                if (args.length == 2) return List.of("set", "reset");
                return onlinePlayers(args[args.length - 1]);
            }
            case "trigger", "fire" -> {
                if (args.length == 2) return new ArrayList<>(plugin.getInteractionManager().getInteractionIds());
                return onlinePlayers(args[args.length - 1]);
            }
            case "clearchat", "untrack" -> { return onlinePlayers(args[args.length - 1]); }
            case "cinematic" -> {
                if (args.length == 2) return List.of("start", "stop");
                if (args.length == 3) return new ArrayList<>(plugin.getInteractionManager().getInteractionIds());
                return onlinePlayers(args[args.length - 1]);
            }
            case "quest" -> { if (args.length == 2) return List.of("track"); }
            case "assets" -> { if (args.length == 2) return List.of("clean"); }
        }

        // Legacy tab completion
        SubCommand legacy = legacySubs.get(args[0].toLowerCase());
        if (legacy != null) return legacy.onTabComplete(sender, args);

        return List.of();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Player resolvePlayer(CommandSender sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player p = Bukkit.getPlayer(args[argIndex]);
            if (p == null) { sender.sendMessage(ChatUtils.toComponent("&cPlayer tidak ditemukan: &f" + args[argIndex])); return null; }
            return p;
        }
        if (sender instanceof Player p) return p;
        sender.sendMessage(ChatUtils.toComponent("&cHarus specify player dari console."));
        return null;
    }

    private List<String> onlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(ChatUtils.toComponent("&cKamu tidak memiliki permission."));
    }
}
