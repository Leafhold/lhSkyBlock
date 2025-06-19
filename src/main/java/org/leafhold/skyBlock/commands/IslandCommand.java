package org.leafhold.skyBlock.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

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
                createIslandGUI(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have an island yet. Use " + ChatColor.YELLOW + "/island create" + ChatColor.RED + " to create one.");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (islandMap.containsKey(uuid)) {
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
                islandHelp(player);
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

    private void islandHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "SkyBlock Commands:");
        player.sendMessage(ChatColor.YELLOW + "/island create" + ChatColor.WHITE + " - Create a new island.");
        player.sendMessage(ChatColor.YELLOW + "/island delete" + ChatColor.WHITE + " - Delete your island.");
        player.sendMessage(ChatColor.YELLOW + "/island home" + ChatColor.WHITE + " - Teleport to your island.");
        player.sendMessage(ChatColor.YELLOW + "/island help" + ChatColor.WHITE + " - Show this help message.");
    }

    private void createIslandGUI(Player player) {
        //todo fetch allow_visitors
        boolean allowVisitors = true; //! Placeholder
        
        Inventory islandGUI = Bukkit.createInventory(player, 54, "Manage island");

        ItemStack home = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta homeMeta = home.getItemMeta();
        homeMeta.setDisplayName(ChatColor.AQUA + "Island home");
        homeMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to teleport to your island."));
        home.setItemMeta(homeMeta);
        islandGUI.setItem(0, home);

        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        membersMeta.setDisplayName(ChatColor.YELLOW + "Members");
        membersMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to view your island members."));
        members.setItemMeta(membersMeta);
        islandGUI.setItem(1, members);

        ItemStack visitors = new ItemStack(Material.RED_CONCRETE);
        ItemMeta visitorsMeta = visitors.getItemMeta();
        visitorsMeta.setDisplayName(ChatColor.AQUA + "Allow visitors");
        if (allowVisitors) {
            visitors = new ItemStack(Material.GREEN_CONCRETE);
            visitorsMeta.setLore(java.util.Arrays.asList(
                ChatColor.GREEN + "ON",
                ChatColor.GRAY + "Click to toggle access to visitors."
                ));
        } else {
            visitors = new ItemStack(Material.RED_CONCRETE);
            visitorsMeta.setLore(java.util.Arrays.asList(
                ChatColor.RED + "OFF",
                ChatColor.GRAY + "Click to toggle access to visitors."
                ));
        }
        visitors.setItemMeta(visitorsMeta);
        islandGUI.setItem(2, visitors);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.RED + "Delete island");
        deleteMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to delete your island."));
        delete.setItemMeta(deleteMeta);
        islandGUI.setItem(3, delete);

        player.openInventory(islandGUI);
    }
}