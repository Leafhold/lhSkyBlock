package org.leafhold.lhSkyBlock.commands;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.crates.CrateHolder;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.command.TabCompleter;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class KeysCommand implements CommandExecutor, TabCompleter {
    private lhSkyBlock plugin;
    private DatabaseManager databaseManager;
    private NPCRegistry npcRegistry;

    public KeysCommand(lhSkyBlock plugin) {
        this.plugin = plugin;
        databaseManager = DatabaseManager.getInstance();
        npcRegistry = CitizensAPI.getNPCRegistry();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /keys setup <NPC_ID>").color(NamedTextColor.RED));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            try {
                int npcId = Integer.parseInt(args[1]);
                NPC npc = npcRegistry.getById(npcId);
                if (npc == null) {
                    player.sendMessage(Component.text("NPC with ID " + npcId + " not found.").color(NamedTextColor.RED));
                    return true;
                }
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid NPC ID: " + args[1]).color(NamedTextColor.RED));
                return true;
            }
        } else {
            player.sendMessage(Component.text("Usage: /keys setup <NPC_ID>").color(NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("setup");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            for (NPC npc : npcRegistry) {
                completions.add(String.valueOf(npc.getId()));
            }
        }
        return completions;
    }
}
