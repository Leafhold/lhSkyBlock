package org.leafhold.skyBlock.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings({"deprecation"})
public class IslandCommand implements CommandExecutor {

    public static final HashMap<UUID, Location> islandMap = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            if (islandMap.containsKey(uuid)) {
                //todo Open island GUI
            } else {
                player.sendMessage(ChatColor.RED + "You do not have an island yet. Use " + ChatColor.YELLOW + "/island create" + ChatColor.RED + " to create one.");
            }
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!(islandMap.containsKey(uuid))) {
                    player.sendMessage(ChatColor.RED + "Your island already exists. Use " + ChatColor.YELLOW + "/island home" + ChatColor.RED + " to teleport to your island.");
                } else {
                    //todo create island
                }
                break;

            case "delete":
                if (islandMap.containsKey(uuid)) {
                    //todo delete island
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have an island to delete. Use " + ChatColor.YELLOW + "/island create" + ChatColor.RED + " to create one.");
                }
                break;

            case "help":
                //todo help message
                break;

            case "home":
                if (islandMap.containsKey(uuid)) {
                    teleportToIsland(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have an island yet. Try " + ChatColor.YELLOW + "/island create" + ChatColor.RED + " to create one.");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Try" + ChatColor.YELLOW + "/island help" + ChatColor.RED + "instead.");
        }
        return true;
    }

    private void teleportToIsland(Player player) {
        Location loc = islandMap.get(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "Teleporting to your island...");
        player.teleport(loc.add(0.5, 1, 0.5));
    }
}