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
 * Renders dialogue using a TypewriterMC-style CHAT display.
 *
 * How it works:
 *  1. Every 3 ticks → reveal next word (typewriter effect)
 *  2. Every 3 ticks (during typewriter) + every 20 ticks (after done):
 *       re-send the entire chat dialogue box, pushing other players'
 *       chat messages up and off screen.
 *
 * Display layers:
 *  - CHAT        → main dialogue box (text + options)
 *  - ACTION BAR  → control hints ("Klik Kanan untuk pilih | Attack untuk konfirmasi")
 *  - BOSS BAR    → handled by TimerController
 *  - HOLOGRAM    → NPC name tag above head (optional, minimal)
 */
public class DialogueRenderer {

    // Number of blank lines sent before the dialogue box to "clear" chat
    private static final int CHAT_CLEAR_LINES = 10;

    // Box width in characters (for the separator line)
    private static final String SEPARATOR = "&8&m                                                  ";

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

    // ─── Tasks & entity ───────────────────────────────────────────────────────
    private BukkitRunnable typewriterTask;
    private BukkitRunnable refreshTask;
    private TextDisplay npcNameDisplay; // Minimal hologram: NPC name only

    public DialogueRenderer(NaturalInteraction plugin, Player player, Interaction interaction) {
        this.plugin = plugin;
        this.player = player;
        this.interaction = interaction;
    }

    // ─── Hologram (NPC name tag only) ─────────────────────────────────────────

    public void initHologram(Location npcBase) {
        clearHologram();
        if (npcBase == null) return;
        Location loc = npcBase.clone().add(0, 2.4, 0);
        String displayName = buildNpcDisplayName();
        npcNameDisplay = loc.getWorld().spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setViewRange(12);
            td.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.text(ChatUtils.toComponent("&6&l✦ &e" + displayName));
        });
    }

    public void clearHologram() {
        if (npcNameDisplay != null && npcNameDisplay.isValid()) npcNameDisplay.remove();
        npcNameDisplay = null;
    }

    // ─── Typewriter + Chat Streaming ──────────────────────────────────────────

    public void startTypewriter(String rawText, List<Option> options) {
        this.currentOptions = options != null ? options : List.of();
        this.selectedOptionIndex = 0;
        this.displayingOptions = false;
        this.fullDialogueText = rawText.replace("%player%", player.getName());
        this.revealedWords = 0;
        this.typewriterDone = false;

        int totalWords = stripColors(fullDialogueText).split(" ").length;

        // Typewriter: reveal words + re-render chat every 3 ticks
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
                    startRefreshTask(); // Hand off to slower refresh task
                }
                renderChat();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
            }
        };
        typewriterTask.runTaskTimer(plugin, 0L, 3L);
    }

    /**
     * Instantly complete the typewriter (first Shift press).
     */
    public void completeInstantly() {
        typewriterDone = true;
        revealedWords = stripColors(fullDialogueText).split(" ").length;
        cancelTypewriter();
        if (!displayingOptions) showOptions();
        renderChat();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
        startRefreshTask();
    }

    /**
     * After typewriter is done, keep re-sending the chat box every 20 ticks (1 second)
     * so other players' messages don't bury the dialogue.
     */
    private void startRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) refreshTask.cancel();
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                renderChat();
            }
        };
        refreshTask.runTaskTimer(plugin, 20L, 20L); // Every 1 second
    }

    public void cancelTypewriter() {
        if (typewriterTask != null && !typewriterTask.isCancelled()) typewriterTask.cancel();
    }

    public void cancelAll() {
        cancelTypewriter();
        if (refreshTask != null && !refreshTask.isCancelled()) refreshTask.cancel();
    }

    public void clearActionBar() {
        player.sendActionBar(Component.empty());
    }

    // ─── Option Selection — Cycle & Confirm ───────────────────────────────────

    public void showOptions() {
        if (currentOptions.isEmpty()) return;
        player.getInventory().setHeldItemSlot(0);
        displayingOptions = true;
        selectedOptionIndex = 0;
        renderHotbar();
        renderChat(); // Re-render chat to show options in dialogue box
    }

    public void cycleNext() {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        selectedOptionIndex = (selectedOptionIndex + 1) % currentOptions.size();
        renderHotbar();
        renderChat(); // Re-render chat immediately to reflect new highlight
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

    public void cyclePrev() {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        selectedOptionIndex = (selectedOptionIndex - 1 + currentOptions.size()) % currentOptions.size();
        renderHotbar();
        renderChat();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

    public void jumpToSlot(int slot) {
        if (!displayingOptions || currentOptions.isEmpty()) return;
        int target = slot % currentOptions.size();
        if (target == selectedOptionIndex) return;
        selectedOptionIndex = target;
        renderHotbar();
        renderChat();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.5f);
    }

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

    // ─── Core Chat Renderer ───────────────────────────────────────────────────

    /**
     * Sends the entire dialogue box to the player's chat.
     *
     * Format:
     * [blank lines × CHAT_CLEAR_LINES]
     * ─────────────────────────────────────────
     * ✦ NPC Name
     *
     *   "Revealed dialogue text here, word by word..."
     *
     *   ▶ Option 1 (highlighted)
     *     Option 2
     *     Option 3
     *
     * ─────────────────────────────────────────
     *   ▶ 1/3 | Klik Kanan/Scroll pilih  |  Attack/"F" konfirmasi
     */
    private void renderChat() {
        // 1. Push other chat up with blank lines
        for (int i = 0; i < CHAT_CLEAR_LINES; i++) {
            player.sendMessage(Component.empty());
        }

        // 2. Top separator
        player.sendMessage(ChatUtils.toComponent(SEPARATOR));

        // 3. NPC name header
        String npcName = buildNpcDisplayName();
        player.sendMessage(ChatUtils.toComponent("  &6&l✦ &e&l" + npcName));
        player.sendMessage(Component.empty());

        // 4. Dialogue text (revealed so far)
        String revealed = getRevealedText(fullDialogueText, revealedWords);
        // Word-wrap at ~45 chars
        for (String line : wordWrap(revealed, 45)) {
            player.sendMessage(ChatUtils.toComponent("  &f" + line));
        }

        // 5. Options (if displaying)
        if (typewriterDone && !currentOptions.isEmpty()) {
            player.sendMessage(Component.empty());
            for (int i = 0; i < currentOptions.size(); i++) {
                boolean selected = (i == selectedOptionIndex);
                String prefix = selected ? "&6&l▶ &e" : "&8  &7";
                String suffix = selected ? " &8&o◀" : "";
                player.sendMessage(ChatUtils.toComponent("  " + prefix
                        + (i + 1) + ". " + currentOptions.get(i).getText() + suffix));
            }
        }

        player.sendMessage(Component.empty());

        // 6. Bottom separator
        player.sendMessage(ChatUtils.toComponent(SEPARATOR));

        // 7. Control hint in ActionBar (always visible, not buried in chat)
        player.sendActionBar(buildControlHint());
    }

    // ─── Private Hotbar Rendering ─────────────────────────────────────────────

    private void renderHotbar() {
        player.getInventory().clear();
        for (int i = 0; i < currentOptions.size(); i++) {
            Option opt = currentOptions.get(i);
            boolean selected = (i == selectedOptionIndex);

            ItemStack item = new ItemStack(selected ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();

            if (selected) {
                meta.displayName(ChatUtils.toComponent("&#FFAA00&l▶ &e" + (i + 1) + ". " + opt.getText()));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(ChatUtils.toComponent("&8  &7" + (i + 1) + ". " + opt.getText()));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);

            player.getInventory().setItem(i % 9, item);
        }
        player.getInventory().setHeldItemSlot(selectedOptionIndex % 9);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Component buildControlHint() {
        String hint;
        if (displayingOptions && !currentOptions.isEmpty()) {
            hint = "&#FFD700▶ &e" + (selectedOptionIndex + 1) + "/" + currentOptions.size()
                    + "  &8│  &fKlik Kanan&8/&fScroll &7pilih  "
                    + "&8│  &fAttack&8/&f\"F\" &7konfirmasi";
        } else if (typewriterDone) {
            hint = "&#FF3333➤ &cShift &funtuk lanjut";
        } else {
            hint = "&#FF3333➤ &cShift &funtuk skip teks";
        }
        return ChatUtils.toComponent(hint);
    }

    private String buildNpcDisplayName() {
        String raw = interaction.getId().replace("_", " ");
        return raw.isEmpty() ? "NPC" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    /** Simple word-wrap: split revealed text into lines of max {@code maxLen} chars (stripped). */
    private java.util.List<String> wordWrap(String text, int maxLen) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        String activeColor = "";

        for (String word : words) {
            String stripped = stripColors(word);
            if (stripColors(line.toString()).length() + stripped.length() + 1 > maxLen && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(activeColor);
            }
            if (!line.isEmpty()) line.append(" ");
            line.append(word);
            activeColor = getActiveColors(line.toString());
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
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
