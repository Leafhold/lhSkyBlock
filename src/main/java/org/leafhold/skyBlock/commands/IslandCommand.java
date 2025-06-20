package org.leafhold.skyBlock.commands;

import org.bukkit.ChatColor;
//! import org.bukkit.Location;  not used
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.leafhold.skyBlock.utils.DatabaseManager;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;

import java.sql.SQLException;

@SuppressWarnings({"deprecation"})
public class IslandCommand implements CommandExecutor, Listener {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            final TextComponent message = Component.text("Only players can use this command.")
                .color(NamedTextColor.RED);
            sender.sendMessage(message);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            checkIsland(player, "create");
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                checkIsland(player, subCommand);
                break;

            case "delete":
                checkIsland(player, subCommand);
                break;
                
            case "help":
                islandHelp(player);
                break;

            case "home":
                checkIsland(player, subCommand);
                break;

            default:
                final TextComponent message = Component.text("Unknown command. Try ")
                    .color(NamedTextColor.RED)
                    .append(
                        Component.text("/island help")
                            .color(NamedTextColor.YELLOW)
                            .hoverEvent(Component.text("Click to see available commands."))
                            .clickEvent(ClickEvent.runCommand("/island help"))
                    )
                    .append(Component.text(" instead.").color(NamedTextColor.RED));
                player.sendMessage(message);
        }
        return true;
    }

    private void islandHelp(Player player) {
        final TextComponent message = Component.text("Available commands:")
            .color(NamedTextColor.GOLD)
            .append(Component.newline())
            .append(
                Component.text("/island create")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/island create"))
                )
            .append(Component.text(" - Create a new island."))
            .append(Component.newline())
            .append(
                Component.text("/island delete")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/island delete"))
                )
            .append(null, Component.text(" - Delete your island."))
            .append(Component.newline()
            .append(
                Component.text("/island home")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/island home"))
                .append(Component.text(" - Teleport to your island.")
                )
            .append(Component.newline())
            .append(
                Component.text("/island help")
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/island help"))
                .append(Component.text(" - Show this help message.")
            ))));
        player.sendMessage(message);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        switch (event.getView().getTitle()) {
            case "Manage island":
                event.setCancelled(true);

                // Check if the player clicked your item
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    
                    switch (event.getCurrentItem().getItemMeta().getDisplayName()) {
                        case "Island home":
                            checkIsland(player, "home");
                            break;

                        case "Allow visitors":
                            // todo toggle allow visitors
                            //player.sendMessage(ChatColor.AQUA + "Denied players to visit your island.");
                            // todo toggle allow visitors
                            //player.sendMessage(ChatColor.AQUA + "Allowed players to visit your island.");
                            break;

                        case "Delete island":
                            checkIsland(player, "delete");
                            break;

                        case "Members":
                            membersGUI(player);
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
                    //todo fetch members
                }  
                break;
            case "Create your island":
                event.setCancelled(true);

                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    switch (event.getCurrentItem().getItemMeta().getDisplayName()) {
                        case "§aCreate Island":
                            //todo createIsland();
                        break;
                    }
                }
                break;
        }
    }
    
    private void checkIsland(Player player, String subCommand) {

        try {
            String islandUUID = DatabaseManager.getInstance().islandUUID(
                player.getUniqueId().toString(),
                player.getName() + "'s Island",
                "islands",
                0,
                0
            );

            final TextComponent errorMessage = Component.text("You do not have an island yet. Use ")
                .color(NamedTextColor.RED)
                .append(
                    Component.text("/island create")
                        .color(NamedTextColor.YELLOW)
                        .hoverEvent(Component.text("Click to create your island."))
                        .clickEvent(ClickEvent.runCommand("/island create"))
                )
                .append(Component.text(" to create one.").color(NamedTextColor.RED));

            switch (subCommand) {
                case "create":
                    if (islandUUID == null) {
                        createIslandGUI(player);
                    } else {
                        islandGUI(player);
                    }
                    break;

                case "home":
                    if (islandUUID != null) {
                        player.sendMessage(ChatColor.AQUA + "Teleporting to your island...");
                        // TODO: teleportToIsland(player);
                    } else {
                        player.sendMessage(errorMessage);
                    }
                    break;

                case "delete":
                    if (islandUUID != null) {
                        player.sendMessage(ChatColor.RED + "Deleting your island...");
                        // TODO: deleteIsland(player);
                    } else {
                        player.sendMessage(errorMessage);
                    }
                    break;

                default:
                    player.sendMessage(ChatColor.RED + "Unknown subcommand.");
                    break;
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while accessing your island.");
        }
    }

    private void createIsland() {
        //todo create island
    }
    
    private void teleportToIsland(Player player) {
        //todo fetch island location from database and teleport player
    }

    private void deleteIsland(Player player) {
        //todo fetch island location from database and delete island
    }

//! GUI

    private void islandGUI(Player player) {

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

    private void membersGUI(Player player) {
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

    private void createIslandGUI(Player player) {
        Inventory createIslandGUI = Bukkit.createInventory(player, 27, "Create your island");

        ItemStack create = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName("§aCreate Island");
        createMeta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Click to create your island."));
        create.setItemMeta(createMeta);
        createIslandGUI.setItem(13, create);

        player.openInventory(createIslandGUI);
    } 

}