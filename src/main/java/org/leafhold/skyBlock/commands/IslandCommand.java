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
import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings({"deprecation"})
public class IslandCommand implements CommandExecutor, Listener {

    public static final HashMap<UUID, UUID> islandMap = new HashMap<>(); //todo remove this, use database

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            final TextComponent message = Component.text("Only players can use this command.")
                .color(NamedTextColor.RED);
            sender.sendMessage(message);
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            if (islandMap.containsKey(uuid)) {
                islandGUI(player);
            } else {
                final TextComponent message = Component.text("You do not have an island yet. Use ")
                    .color(NamedTextColor.RED)
                    .append(
                        Component.text("/island create")
                            .color(NamedTextColor.YELLOW)
                            .hoverEvent(Component.text("Click to create your island."))
                            .clickEvent(ClickEvent.runCommand("/island create"))
                    )
                    .append(Component.text(" to create one.").color(NamedTextColor.RED));
                    
                player.sendMessage(message);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (islandMap.containsKey(uuid)) {
                    final TextComponent message = Component.text("You already have an island. Use ")
                        .color(NamedTextColor.RED)
                        .append(
                            Component.text("/island home")
                                .color(NamedTextColor.YELLOW)
                                .hoverEvent(Component.text("Click to teleport to your island."))
                                .clickEvent(ClickEvent.runCommand("/island home"))
                        )
                        .append(Component.text(" to teleport to your island.").color(NamedTextColor.RED));
                    player.sendMessage(message);
                } else {
                    createIslandGUI(player);
                }
                break;

            case "delete":
                if (islandMap.containsKey(uuid)) {
                    //todo delete island
                } else {
                    final TextComponent message = Component.text("You do not have an island to delete. Use ")
                        .color(NamedTextColor.RED)
                        .append(
                            Component.text("/island create")
                                .color(NamedTextColor.YELLOW)
                                .hoverEvent(Component.text("Click to create your island."))
                                .clickEvent(ClickEvent.runCommand("/island create"))
                        )
                        .append(Component.text(" to create one.").color(NamedTextColor.RED));
                    player.sendMessage(message);
                }
                break;
                
            case "help":
                islandHelp(player);
                break;

            case "home":
                if (islandMap.containsKey(uuid)) {
                    teleportToIsland(player);
                } else {
                    final TextComponent message = Component.text("You do not have an island yet. Use ")
                        .color(NamedTextColor.RED)
                        .append(
                            Component.text("/island create")
                                .color(NamedTextColor.YELLOW)
                                .hoverEvent(Component.text("Click to create your island."))
                                .clickEvent(ClickEvent.runCommand("/island create"))
                        )
                        .append(Component.text(" to create one.").color(NamedTextColor.RED));
                    player.sendMessage(message);
                }
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

    private void teleportToIsland(Player player) {
        //todo fetch island location from database and teleport player
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
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        Player player = (Player) event.getWhoClicked();

        switch (event.getView().getTitle()) {
            case "Manage island":
                event.setCancelled(true);

                // Check if the player clicked your item
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    
                    switch (event.getCurrentItem().getItemMeta().getDisplayName()) {
                        case "Island home":
                            // todo player.teleport(islandMap.get(player.getUniqueId()).add(0.5, 1, 0.5));
                            player.sendMessage(ChatColor.AQUA + "Teleporting to your island...");
                            break;

                        case "Allow visitors":
                            // todo toggle allow visitors
                            //player.sendMessage(ChatColor.AQUA + "Denied players to visit your island.");
                            // todo toggle allow visitors
                            //player.sendMessage(ChatColor.AQUA + "Allowed players to visit your island.");
                            break;

                        case "Delete island":
                            //todo delete island
                            player.sendMessage(ChatColor.AQUA + "Deleting your island.");
                            break;

                        case "Members":
                            //todo members GUI
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
                    player.sendMessage("IT WORKS!");
                }  
                break;
            case "Create your island":
                event.setCancelled(true);

                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    switch (event.getCurrentItem().getItemMeta().getDisplayName()) {
                        case "§aCreate Island":
                            //todo create island
                            player.sendMessage(ChatColor.GREEN + "Creating your island...");
                            String islandUUID = DatabaseManager.getInstance().createIsland(
                                player.getUniqueId().toString(), 
                                player.getName() + "'s Island",
                                "islands", //todo add world selection logic
                                0,
                                0
                            );
                            if (islandUUID != null) {
                                player.sendMessage(ChatColor.GREEN + "Island created successfully!");
                                //todo teleport to island
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "Failed to create island. You might already have one.");
                            }
                        break;
                    }
                }
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown item clicked!");
                break;
        }
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