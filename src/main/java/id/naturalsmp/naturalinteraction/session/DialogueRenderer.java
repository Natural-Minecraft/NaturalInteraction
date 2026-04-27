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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Renders all visual output during a dialogue session:
 *  - Typewriter effect on ActionBar
 *  - TextDisplay hologram above NPC head
 *  - Hotbar option items with cycle-and-confirm selection
 *
 * Selection model (TypewriterMC-style):
 *  - Right-click / Scroll     → cycle through options
 *  - Left-click / "F" key     → confirm highlighted option
 *  - Hotbar slots 1–N         → direct jump to option index (infinite scroll wrap)
 */
public class DialogueRenderer {

    private final NaturalInteraction plugin;
    private final Player player;
    private final Interaction interaction;

    // ─── Typewriter state ─────────────────────────────────────────────────────
    private String fullDialogueText = "";
    private int revealedWords = 0;
    private boolean typewriterDone = false;

    // ─── Selection state ──────────────────────────────────────────────────────
    private List<Option> currentOptions = List.of();
    private int selectedOptionIndex = 0;
    private boolean displayingOptions = false;

    // ─── Active tasks & entity ────────────────────────────────────────────────
    private BukkitRunnable typewriterTask;
    private BukkitRunnable actionBarTask;
    private TextDisplay mainTextDisplay;

    public DialogueRenderer(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;
    }

    // ─── Hologram ─────────────────────────────────────────────────────────────

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

    public void startTypewriter(String rawText, List<Option> options) {
        this.currentOptions = options != null ? options : List.of();
        this.selectedOptionIndex = 0;
        this.displayingOptions = false;
        this.fullDialogueText = rawText.replace("%player%", player.getName());
        this.revealedWords = 0;
        this.typewriterDone = false;

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
                    if (!displayingOptions) showOptions();
                    cancel();
                }
                String revealed = getRevealedText(fullDialogueText, revealedWords);
                String padding = " ".repeat(Math.max(0,
                        stripColors(fullDialogueText).length() - stripColors(revealed).length()));
                player.sendActionBar(buildActionBar(unicode, prefix, revealed, padding));
                updateHologram(revealed);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
            }
        };
        typewriterTask.runTaskTimer(plugin, 0L, 3L);

        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (!typewriterDone) return;
                String revealed = getRevealedText(fullDialogueText, revealedWords);
                String padding = " ".repeat(Math.max(0,
                        stripColors(fullDialogueText).length() - stripColors(revealed).length()));
                player.sendActionBar(buildActionBar(unicode, prefix, revealed, padding));
                updateHologram(revealed);
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void completeInstantly() {
        typewriterDone = true;
        revealedWords = stripColors(fullDialogueText).split(" ").length;
        cancelTypewriter();
        if (!displayingOptions) showOptions();
        String unicode = interaction.getDialogueUnicode();
        String prefix = unicode.isEmpty() ? "" : unicode + " ";
        player.sendActionBar(buildActionBar(unicode, prefix, fullDialogueText, ""));
        updateHologram(fullDialogueText);
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

    // ─── Option Selection — Cycle & Confirm ───────────────────────────────────

    /**
     * Show options in hotbar. Called automatically when typewriter completes.
     * Each slot corresponds to an option index (infinite scroll wraps).
     */
    public void showOptions() {
        if (currentOptions.isEmpty()) return;
        player.getInventory().setHeldItemSlot(0);
        displayingOptions = true;
        selectedOptionIndex = 0;
        renderHotbar();
    }

    /**
     * Cycle to the next option (Right-click / Scroll Down).
     * Wraps around infinitely.
     */
    public void cycleNext() {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        selectedOptionIndex = (selectedOptionIndex + 1) % currentOptions.size();
        renderHotbar();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

    /**
     * Cycle to the previous option (Scroll Up).
     * Wraps around infinitely.
     */
    public void cyclePrev() {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        selectedOptionIndex = (selectedOptionIndex - 1 + currentOptions.size()) % currentOptions.size();
        renderHotbar();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

    /**
     * Jump directly to a specific option index via hotbar slot.
     * Uses modulo so it wraps when options > 9.
     */
    public void jumpToSlot(int slot) {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        int target = slot % currentOptions.size();
        if (target == selectedOptionIndex) return;
        selectedOptionIndex = target;
        renderHotbar();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

    /**
     * Confirm the currently highlighted option.
     * Returns the selected Option, or null if none / not yet displaying.
     */
    public Option confirmSelected() {
        if (!displayingOptions || currentOptions.isEmpty()) return null;
        if (selectedOptionIndex < 0 || selectedOptionIndex >= currentOptions.size()) return null;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        return currentOptions.get(selectedOptionIndex);
    }

    // ─── State ────────────────────────────────────────────────────────────────

    public boolean isTypewriterDone()    { return typewriterDone; }
    public boolean isDisplayingOptions() { return displayingOptions; }
    public int getSelectedOptionIndex()  { return selectedOptionIndex; }
    public List<Option> getCurrentOptions() { return currentOptions; }

    // ─── Private Hotbar Rendering ─────────────────────────────────────────────

    /**
     * Render all options into the hotbar.
     *
     * Visual:
     *  - Selected → LIME_DYE with enchant glow, gold name, "▶ Option Text"
     *  - Others   → GRAY_DYE, gray name, "  Option Text"
     */
    private void renderHotbar() {
        player.getInventory().clear();
        for (int i = 0; i < currentOptions.size(); i++) {
            Option opt = currentOptions.get(i);
            boolean selected = (i == selectedOptionIndex);

            ItemStack item = new ItemStack(selected ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();

            if (selected) {
                String name = "&#FFAA00&l▶ &e" + opt.getText();
                meta.displayName(ChatUtils.toComponent(name));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                String name = "&8  " + opt.getText();
                meta.displayName(ChatUtils.toComponent(name));
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);

            // Wrap slot index (only 9 slots in hotbar)
            int slot = i % 9;
            player.getInventory().setItem(slot, item);
        }

        // Update hotbar held slot to visually follow selected index
        player.getInventory().setHeldItemSlot(selectedOptionIndex % 9);

        // Update hologram to show current highlighted option
        updateHologram(getRevealedText(fullDialogueText, revealedWords));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void updateHologram(String revealedText) {
        if (mainTextDisplay == null || !mainTextDisplay.isValid()) return;
        StringBuilder sb = new StringBuilder(revealedText);
        if (typewriterDone && !currentOptions.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < currentOptions.size(); i++) {
                boolean selected = (i == selectedOptionIndex);
                if (selected) {
                    sb.append("\n&#FFAA00&l▶ ").append(i + 1).append(". &e").append(currentOptions.get(i).getText());
                } else {
                    sb.append("\n&8  ").append(i + 1).append(". &7").append(currentOptions.get(i).getText());
                }
            }
        }
        mainTextDisplay.text(ChatUtils.toComponent(sb.toString().trim()));
    }

    private Component buildActionBar(String unicode, String prefix, String revealed, String padding) {
        if (unicode == null || unicode.trim().isEmpty()) {
            String instruction;
            if (displayingOptions && !currentOptions.isEmpty()) {
                // Show confirm instructions
                instruction = "&#FFD700▶ &e" + (selectedOptionIndex + 1) + "/" + currentOptions.size()
                        + " &8| &fKlik Kanan&8/&fScroll &7untuk pilih   "
                        + "&8| &fAttack&8/&f\"F\" &7untuk konfirmasi";
            } else if (typewriterDone) {
                instruction = "&#FF3333➤ &cTekan Shift &funtuk skip / lanjut";
            } else {
                instruction = "&#FF3333➤ &cTekan Shift &funtuk skip teks";
            }
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
