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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings({"deprecation"})
public class IslandCommand implements CommandExecutor, Listener {

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

            //! remove
            createIslandGUI(player);
            
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
        boolean allowVisitors = false;
        
        Inventory islandGUI = Bukkit.createInventory(player, 27, "Manage island");

        ItemStack home = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta homeMeta = home.getItemMeta();
        homeMeta.setDisplayName(ChatColor.AQUA + "Island home");
        homeMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to teleport to your island."));
        home.setItemMeta(homeMeta);
        islandGUI.setItem(11, home);

        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        membersMeta.setDisplayName(ChatColor.YELLOW + "Members");
        membersMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to view your island members."));
        members.setItemMeta(membersMeta);
        islandGUI.setItem(22, members);

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
        islandGUI.setItem(4, visitors);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.RED + "Delete island");
        deleteMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to delete your island."));
        delete.setItemMeta(deleteMeta);
        islandGUI.setItem(15, delete);

        player.openInventory(islandGUI);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        switch (event.getView().getTitle()) {
            case "Manage island":
                event.setCancelled(true);

                // Check if the player clicked your item
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    
                    switch (event.getCurrentItem().getType()) {
                        case GRASS_BLOCK:
                            // todo player.teleport(islandMap.get(player.getUniqueId()).add(0.5, 1, 0.5));
                            player.sendMessage(ChatColor.AQUA + "Teleporting to your island...");
                            break;

                        case RED_CONCRETE:
                            // todo toggle allow visitors
                            player.sendMessage(ChatColor.AQUA + "Denied players to visit your island.");
                            break;

                        case GREEN_CONCRETE:
                            // todo toggle allow visitors
                            player.sendMessage(ChatColor.AQUA + "Allowed players to visit your island.");
                            break;

                        case BARRIER:
                            //todo delete island
                            player.sendMessage(ChatColor.AQUA + "Deleting your island.");
                            break;

                        case PLAYER_HEAD:
                            //todo members GUI
                            createMembersGUI(player);
                            break;

                        default:
                            player.sendMessage(ChatColor.RED + "Unknown item clicked!");
                            break;
                    }
                }
                break;
            case "Manage island members":
                event.setCancelled(true);
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    player.sendMessage("IT WORKS!");
                }  
                break;
            default:
                // * Do nothing for other inventories
                break;
        }
    }
    
    private void createMembersGUI(Player player) {
        Inventory membersGUI = Bukkit.createInventory(player, 27, "Manage island members");

        ItemStack players = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playersMeta = players.getItemMeta();
        playersMeta.setDisplayName(ChatColor.YELLOW + "Player");
        players.setItemMeta(playersMeta);

        for (int i = 10; i < 17; i++) {
            membersGUI.setItem(i, players);
        }

        player.openInventory(membersGUI);
    }
}