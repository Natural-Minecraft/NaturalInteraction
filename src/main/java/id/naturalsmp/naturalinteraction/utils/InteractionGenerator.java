package id.naturalsmp.naturalinteraction.utils;

import id.naturalsmp.naturalinteraction.NaturalInteraction;
import id.naturalsmp.naturalinteraction.manager.InteractionManager;
import id.naturalsmp.naturalinteraction.model.Action;
import id.naturalsmp.naturalinteraction.model.ActionType;
import id.naturalsmp.naturalinteraction.model.DialogueNode;
import id.naturalsmp.naturalinteraction.model.Interaction;
import id.naturalsmp.naturalinteraction.model.Option;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InteractionGenerator {

    private final NaturalInteraction plugin;

    public InteractionGenerator(NaturalInteraction plugin) {
        this.plugin = plugin;
    }

    public boolean generate(String id, InteractionTemplate template, Player creator) {
        InteractionManager manager = plugin.getInteractionManager();
        if (manager.hasInteraction(id)) {
            return false;
        }

        Interaction interaction = new Interaction(id);

        switch (template) {
            case QUEST -> generateQuest(interaction);
            case SHOP -> generateShop(interaction);
            case LORE -> generateLore(interaction);
            case TUTORIAL -> generateTutorial(interaction);
        }

        manager.saveInteraction(interaction);
        manager.loadInteractions(); // Reload to register
        return true;
    }

    private void generateQuest(Interaction interaction) {
        // Node 1: Greeting
        DialogueNode n1 = new DialogueNode("start", "&6[NPC] &fHello there! I have a task for you.");
        n1.setDurationSeconds(5);

        Option o1 = new Option("Time is money, friend.", "tell_more", ChatColor.YELLOW);
        Option o2 = new Option("Not interested.", "exit", ChatColor.RED);
        n1.getOptions().add(o1);
        n1.getOptions().add(o2);

        // Node 2: Details
        DialogueNode n2 = new DialogueNode("tell_more", "&6[NPC] &fI need 10 &bWolf Pelts&f. Can you get them?");
        n2.setDurationSeconds(5);

        Option o3 = new Option("I accept!", "accept", ChatColor.GREEN);
        Option o4 = new Option("Too hard.", "exit", ChatColor.RED);
        n2.getOptions().add(o3);
        n2.getOptions().add(o4);

        // Node 3: Accepted
        DialogueNode n3 = new DialogueNode("accept", "&6[NPC] &fGreat! Come back when you have them.");
        n3.setDurationSeconds(3);

        // Actions
        Action giveQuest = new Action(ActionType.COMMAND, "questbook add " + interaction.getId());
        n3.getActions().add(giveQuest);

        interaction.setRootNodeId("start");
        interaction.addNode(n1);
        interaction.addNode(n2);
        interaction.addNode(n3);
    }

    private void generateShop(Interaction interaction) {
        // Node 1: Greeting
        DialogueNode n1 = new DialogueNode("start", "&6[Shopkeeper] &fWelcome! Want to trade?");

        // Interactive options
        Option o1 = new Option("Browse Wares", "browse", ChatColor.GOLD);
        Option o2 = new Option("Sell Items", "sell", ChatColor.AQUA);
        Option o3 = new Option("Goodbye", "exit", ChatColor.RED);

        n1.getOptions().add(o1);
        n1.getOptions().add(o2);
        n1.getOptions().add(o3);

        // Node 2: Browse Action (Placeholder)
        DialogueNode n2 = new DialogueNode("browse", "&7Opening shop...");
        n2.setDurationSeconds(1);
        n2.getActions().add(new Action(ActionType.COMMAND, "shop open"));

        // Node 3: Sell Action
        DialogueNode n3 = new DialogueNode("sell", "&7Opening sell menu...");
        n3.setDurationSeconds(1);
        n3.getActions().add(new Action(ActionType.COMMAND, "shop sell"));

        interaction.setRootNodeId("start");
        interaction.addNode(n1);
        interaction.addNode(n2);
        interaction.addNode(n3);
    }

    private void generateLore(Interaction interaction) {
        // Node 1
        DialogueNode n1 = new DialogueNode("start", "&6[Elder] &fLong ago, this land was peaceful...");
        n1.setNextNodeId("part2");
        n1.setDelayBeforeNext(60); // 3s

        // Node 2
        DialogueNode n2 = new DialogueNode("part2", "&6[Elder] &fThen the Fire Nation attacked.");
        n2.setNextNodeId("part3");
        n2.setDelayBeforeNext(60);

        // Node 3
        DialogueNode n3 = new DialogueNode("part3", "&6[Elder] &fOnly the Avatar could stop them.");

        interaction.setRootNodeId("start");
        interaction.addNode(n1);
        interaction.addNode(n2);
        interaction.addNode(n3);
    }

    private void generateTutorial(Interaction interaction) {
        // Simple linear guide
        DialogueNode n1 = new DialogueNode("start", "&e[Guide] &fWelcome to NaturalSMP! Click NEXT to continue.");
        n1.getOptions().add(new Option("NEXT >>", "step2", ChatColor.GREEN));

        DialogueNode n2 = new DialogueNode("step2", "&e[Guide] &fUse /rtp to find a place to build.");
        n2.getOptions().add(new Option("NEXT >>", "step3", ChatColor.GREEN));

        DialogueNode n3 = new DialogueNode("step3", "&e[Guide] &fClaim your land with a golden shovel.");
        n3.getOptions().add(new Option("FINISH", "exit", ChatColor.AQUA));

        interaction.setRootNodeId("start");
        interaction.addNode(n1);
        interaction.addNode(n2);
        interaction.addNode(n3);
    }
}
