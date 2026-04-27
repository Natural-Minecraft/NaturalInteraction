package id.naturalsmp.naturalinteraction.session;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.Option;
import id.naturalsmp.naturalinteraction.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Renders all visual output during a dialogue session:
 *  - Typewriter effect on ActionBar
 *  - TextDisplay hologram above NPC head
 *  - Hotbar option items
 *
 * Extracted from InteractionSession to separate the "rendering" concern.
 */
public class DialogueRenderer {

    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;

    // Typewriter state
    private String fullDialogueText = "";
    private int revealedWords = 0;
    private boolean typewriterDone = false;
    private boolean displayingOptions = false;

    // Active tasks & entity
    private BukkitRunnable typewriterTask;
    private BukkitRunnable actionBarTask;
    private TextDisplay mainTextDisplay;

    public DialogueRenderer(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;
    }

    // ─── Hologram ─────────────────────────────────────────────────────────────

    /** Spawn (or re-spawn) the TextDisplay hologram at the given NPC base location. */
    public void initHologram(Location npcBase) {
        clearHologram();
        if (npcBase == null) return;
        Location loc = npcBase.clone().add(0, 2.8, 0);
        mainTextDisplay = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setViewRange(15);
            td.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
    }

    public void clearHologram() {
        if (mainTextDisplay != null && mainTextDisplay.isValid()) mainTextDisplay.remove();
        mainTextDisplay = null;
    }

    // ─── Typewriter ───────────────────────────────────────────────────────────

    /**
     * Begin the typewriter effect. Call {@link #initHologram(Location)} first.
     */
    public void startTypewriter(String rawText, List<Option> options) {
        fullDialogueText = rawText.replace("%player%", player.getName());
        revealedWords = 0;
        typewriterDone = false;
        displayingOptions = false;

        String unicode = interaction.getDialogueUnicode();
        String prefix = unicode.isEmpty() ? "" : unicode + " ";
        int totalWords = stripColors(fullDialogueText).split(" ").length;

        typewriterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                revealedWords++;
                if (revealedWords >= totalWords) {
                    revealedWords = totalWords;
                    typewriterDone = true;
                    if (!displayingOptions) displayHotbarOptions(options);
                    cancel();
                }
                String revealed = getRevealedText(fullDialogueText, revealedWords);
                String padding = " ".repeat(Math.max(0,
                        stripColors(fullDialogueText).length() - stripColors(revealed).length()));
                player.sendActionBar(buildActionBar(unicode, prefix, revealed, padding, options));
                updateHologram(revealed, options);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
            }
        };
        typewriterTask.runTaskTimer(plugin, 0L, 3L);

        // Keep ActionBar alive after typewriter finishes (fades after ~2s)
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (!typewriterDone) return;
                String revealed = getRevealedText(fullDialogueText, revealedWords);
                String padding = " ".repeat(Math.max(0,
                        stripColors(fullDialogueText).length() - stripColors(revealed).length()));
                player.sendActionBar(buildActionBar(unicode, prefix, revealed, padding, options));
                updateHologram(revealed, options);
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Instantly complete the typewriter (first skip press).
     * Shows full text immediately and reveals hotbar options.
     */
    public void completeInstantly(List<Option> options) {
        typewriterDone = true;
        revealedWords = stripColors(fullDialogueText).split(" ").length;
        cancelTypewriter();
        if (!displayingOptions) displayHotbarOptions(options);
        String unicode = interaction.getDialogueUnicode();
        String prefix = unicode.isEmpty() ? "" : unicode + " ";
        player.sendActionBar(buildActionBar(unicode, prefix, fullDialogueText, "", options));
        updateHologram(fullDialogueText, options);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
    }

    public void cancelTypewriter() {
        if (typewriterTask != null && !typewriterTask.isCancelled()) typewriterTask.cancel();
    }

    public void cancelAll() {
        cancelTypewriter();
        if (actionBarTask != null && !actionBarTask.isCancelled()) actionBarTask.cancel();
    }

    public void clearActionBar() {
        player.sendActionBar(Component.empty());
    }

    // ─── Hotbar Options ───────────────────────────────────────────────────────

    public void displayHotbarOptions(List<Option> options) {
        if (options == null || options.isEmpty()) return;
        player.getInventory().setHeldItemSlot(4);
        displayingOptions = true;
        player.getInventory().clear();
        for (int i = 0; i < Math.min(options.size(), 9); i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ChatUtils.toComponent("&#FFAA00&lPilih: &f" + options.get(i).getText()));
            item.setItemMeta(meta);
            player.getInventory().setItem(i, item);
        }
    }

    // ─── State ────────────────────────────────────────────────────────────────

    public boolean isTypewriterDone()    { return typewriterDone; }
    public boolean isDisplayingOptions() { return displayingOptions; }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void updateHologram(String revealedText, List<Option> options) {
        if (mainTextDisplay == null || !mainTextDisplay.isValid()) return;
        StringBuilder sb = new StringBuilder(revealedText);
        if (typewriterDone && options != null && !options.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < options.size(); i++) {
                sb.append("\n&#FFAA00&l").append(i + 1).append(" &#FFFF55>> &e")
                  .append(options.get(i).getText());
            }
        }
        mainTextDisplay.text(ChatUtils.toComponent(sb.toString().trim()));
    }

    private Component buildActionBar(String unicode, String prefix, String revealed,
                                     String padding, List<Option> options) {
        if (unicode == null || unicode.trim().isEmpty()) {
            String instruction = (options != null && !options.isEmpty())
                    ? "&#FF3333➤ &cKlik Hotbar 1-" + Math.min(options.size(), 8)
                      + " &funtuk memilih   &8|   &#FF3333➤ &cShift &funtuk skip"
                    : "&#FF3333➤ &cTekan Shift &funtuk skip interaksi";
            return ChatUtils.toComponent(instruction);
        }
        return ChatUtils.toComponent(prefix + revealed + padding);
    }

    private String getRevealedText(String coloredText, int wordCount) {
        String[] words = coloredText.split(" ");
        if (wordCount >= words.length) return coloredText;
        StringBuilder result = new StringBuilder();
        String activeColor = "";
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) result.append(" ");
            if (!words[i].startsWith("&") && !words[i].startsWith("§")) result.append(activeColor);
            result.append(words[i]);
            activeColor = getActiveColors(result.toString());
        }
        return result.toString();
    }

    private String getActiveColors(String text) {
        String active = "";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (c == '&' && next == '#' && i + 8 < text.length()) {
                    active = text.substring(i, i + 9); i += 8;
                } else if ("0123456789abcdefABCDEF".indexOf(next) != -1) {
                    active = text.substring(i, i + 2); i++;
                } else if ("klmnorKLMNOR".indexOf(next) != -1) {
                    active = (next == 'r' || next == 'R') ? text.substring(i, i + 2) : active + text.substring(i, i + 2);
                    i++;
                }
            }
        }
        return active;
    }

    private String stripColors(String text) {
        return text.replaceAll("&#[A-Fa-f0-9]{6}", "")
                   .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                   .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                   .replaceAll("<[^>]+>", "");
    }
}
