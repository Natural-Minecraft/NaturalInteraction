package id.naturalsmp.naturalinteraction.session;

import id.naturalsmp.naturalinteraction.manager.CompletionTracker;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Dispatches interaction rewards (items and commands) at session end.
 * Also handles per-node command rewards.
 * Checks one-time reward constraints before giving anything.
 */
public final class RewardDispatcher {

    private RewardDispatcher() {}

    /**
     * Give end-of-interaction rewards to the player.
     *
     * @param player           Recipient
     * @param interaction      The interaction whose rewards to dispatch
     * @param snapshot         Active inventory snapshot (items inserted here; overflow drops naturally)
     * @param tracker          Completion tracker for one-time checks
     * @param alreadyCompleted Whether this player had already completed this interaction
     */
    public static void dispatch(Player player, Interaction interaction,
                                InventorySnapshot snapshot, CompletionTracker tracker,
                                boolean alreadyCompleted) {
        boolean shouldGive = !interaction.isOneTimeReward() || !alreadyCompleted;

        if (!shouldGive) {
            if (alreadyCompleted) {
                player.sendMessage(Component.text("✦ ", NamedTextColor.YELLOW)
                        .append(Component.text("Kamu sudah pernah menyelesaikan interaksi ini.",
                                NamedTextColor.GRAY)));
            }
            return;
        }

        boolean given = false;

        // ── Item Rewards ────────────────────────────────────────────────────
        List<ItemStack> items = interaction.getRewards();
        if (items != null && !items.isEmpty()) {
            for (ItemStack item : items) {
                if (item == null) continue;
                if (snapshot != null) {
                    ItemStack overflow = snapshot.addItem(item);
                    if (overflow != null) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                    }
                } else {
                    player.getInventory().addItem(item);
                }
            }
            given = true;
        }

        // ── Command Rewards ─────────────────────────────────────────────────
        List<String> cmds = interaction.getCommandRewards();
        if (cmds != null && !cmds.isEmpty()) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (String cmd : cmds) {
                Bukkit.dispatchCommand(console,
                        cmd.replace("%player_name%", player.getName())
                           .replace("%player%", player.getName()));
            }
            given = true;
        }

        if (given) {
            player.updateInventory();
            player.sendMessage(Component.text("✨ ", NamedTextColor.GOLD)
                    .append(Component.text("Kamu mendapatkan hadiah!", NamedTextColor.GREEN)));
        }
    }

    /**
     * Dispatch per-node command rewards (only if {@code giveReward=true} on that node).
     * Respects the interaction-level one-time reward flag.
     */
    public static void dispatchNodeRewards(Player player, DialogueNode node,
                                           Interaction interaction, CompletionTracker tracker) {
        if (!node.isGiveReward() || node.getCommandRewards().isEmpty()) return;

        if (interaction.isOneTimeReward()
                && tracker.hasCompleted(player.getUniqueId(), interaction.getId())) return;

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String cmd : node.getCommandRewards()) {
            Bukkit.dispatchCommand(console,
                    cmd.replace("%player_name%", player.getName())
                       .replace("%player%", player.getName()));
        }
        player.sendMessage(Component.text("✨ ", NamedTextColor.GOLD)
                .append(Component.text("Hadiah node diterima!", NamedTextColor.GREEN)));
    }
}
