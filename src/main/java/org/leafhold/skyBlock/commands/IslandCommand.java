package org.leafhold.skyBlock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class IslandCommand implements CommandExecutor, Listener {
    private DatabaseManager databaseManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            final TextComponent message = Component.text("Only players can use this command.")
                .color(NamedTextColor.RED);
            sender.sendMessage(message);
            return true;
        }
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance();
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        List<Object> userIslands = new ArrayList<>();
        try {
            userIslands = databaseManager.getIslandsByOwner(uuid.toString());
        } catch (SQLException e) {
            player.sendMessage(NamedTextColor.RED + "An error occurred while fetching your islands. Please try again later.");
            e.printStackTrace();
            return true;
        }
        if (args.length == 0) {
            if (!userIslands.isEmpty()) {
                if (userIslands.size() == 1) {
                    Object island = userIslands.get(0);
                    Object[] islandObj = (Object[]) island;
                    UUID islandUUID = UUID.fromString(islandObj[0].toString());
                    manageIslandGUI(player, islandUUID);
                } else {
                    islandSelectGUI(player);
                }
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
                if (!userIslands.isEmpty()) {
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
                if (!userIslands.isEmpty()) {
                    Inventory islandDeleteGUI = Bukkit.createInventory(player, 9, Component.text("Delete your island"));
                    List<ItemStack> items = new ArrayList<>();
                    for (Object island : userIslands) {
                        Object[] islandArr = (Object[]) island;
                        String islandName = islandArr[2].toString();
                        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
                        ItemMeta itemMeta = item.getItemMeta();
                        itemMeta.displayName(Component.text(islandName).color(NamedTextColor.GREEN));
                        itemMeta.lore(java.util.Collections.singletonList(Component.text("Click to delete this island.").color(NamedTextColor.GRAY)));
                        itemMeta.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey("skyblock", "uuid"),
                            PersistentDataType.STRING,
                            islandArr[0].toString()
                        );
                        item.setItemMeta(itemMeta);
                        items.add(item);
                    }
                    for (int i = 0; i < items.size(); i++) {
                        islandDeleteGUI.setItem(i, items.get(i));
                    }
                    player.openInventory(islandDeleteGUI);
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
                if (!userIslands.isEmpty()) {
                    //todo teleportToIsland(player);
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
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        Player player = (Player) event.getWhoClicked();

        switch (event.getView().title().toString()) {
            case "Manage island":
                event.setCancelled(true);

                // Check if the player clicked your item
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    
                    switch (event.getCurrentItem().getItemMeta().displayName().toString()) {
                        case "Island home":
                            // todo player.teleport(islandMap.get(player.getUniqueId()).add(0.5, 1, 0.5));
                            player.sendMessage(NamedTextColor.AQUA + "Teleporting to your island...");
                            break;

                        case "Allow visitors":
                            // todo toggle access to visitors
                            break;

                        case "Members":
                            membersGUI(player);
                            break;
                        default:
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
                    switch (event.getCurrentItem().getItemMeta().displayName().toString()) {
                        case "Create Island":
                            //todo create island
                            player.sendMessage(NamedTextColor.GREEN + "Creating your island...");
                            String islandUUID = DatabaseManager.getInstance().createIsland(
                                player.getUniqueId().toString(), 
                                player.getName() + "'s Island",
                                "islands", //todo add world selection logic
                                0,
                                0
                            );
                            if (islandUUID != null) {
                                player.sendMessage(NamedTextColor.GREEN + "Island created successfully!");
                                //todo teleport to island
                            }
                            else {
                                player.sendMessage(NamedTextColor.RED + "Failed to create island. You might already have one.");
                            }
                        break;
                    }
                }
                break;
            
            case "Select island":
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    UUID islandUUID = UUID.fromString(event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                        .get(new org.bukkit.NamespacedKey("skyblock", "uuid"), PersistentDataType.STRING));
                    manageIslandGUI(player, islandUUID);
                }
                break;
        }
    }

    private void islandSelectGUI(Player player) {
        List<Object> userIslands = new ArrayList<>();
        try {
            userIslands = databaseManager.getIslandsByOwner(player.getUniqueId().toString());
        } catch (SQLException e) {
            player.sendMessage(NamedTextColor.RED + "An error occurred while fetching your islands. Please try again later.");
            e.printStackTrace();
            return;
        }
        Inventory islandSelectGUI = Bukkit.createInventory(player, 27, Component.text("Select island"));

        List<ItemStack> islandItems = new ArrayList<>();

        for (Object island : userIslands) {
            Object[] islandObj = (Object[]) island;
            UUID islandUUID = UUID.fromString(islandObj[0].toString());
            String islandName = islandObj[2].toString();
            ItemStack islandItem = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta islandMeta = islandItem.getItemMeta();
            islandMeta.displayName(Component.text(islandName).color(NamedTextColor.AQUA));
            islandMeta.lore(java.util.Arrays.asList(
                Component.text("Click to teleport to your island.").color(NamedTextColor.GRAY),
                Component.text("Right-click to manage your island.").color(NamedTextColor.GRAY)
            ));
            islandMeta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("skyblock", "uuid"),
                PersistentDataType.STRING,
                islandUUID.toString()
            );
            islandItem.setItemMeta(islandMeta);
            islandItems.add(islandItem);
        }

        for (int i = 0; i < userIslands.size(); i++) {
            islandSelectGUI.setItem(i, islandItems.get(i));
        }

        player.openInventory(islandSelectGUI);   
    }

    private void manageIslandGUI(Player player, UUID islandUUID) {
        Object island = null;
        try {
            island = databaseManager.getIslandByUUID(islandUUID);
        } catch (SQLException e) {
            player.sendMessage(NamedTextColor.RED + "An error occurred while fetching your islands. Please try again later.");
            e.printStackTrace();
            return;
        }
        Object[] islandObj = (Object[]) island;
        boolean allowVisitors = Boolean.parseBoolean(islandObj[3].toString());

        Inventory islandGUI = Bukkit.createInventory(player, 27, Component.text("Manage island"));

        ItemStack home = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta homeMeta = home.getItemMeta();
        homeMeta.displayName(Component.text("Island home").color(NamedTextColor.AQUA));
        homeMeta.lore(java.util.Collections.singletonList(Component.text("Click to teleport to your island.").color(NamedTextColor.GRAY)));
        home.setItemMeta(homeMeta);
        islandGUI.setItem(11, home);

        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        membersMeta.displayName(Component.text("Members").color(NamedTextColor.WHITE));
        membersMeta.lore(java.util.Collections.singletonList(Component.text("Click to view your island members.").color(NamedTextColor.GRAY)));
        members.setItemMeta(membersMeta);
        islandGUI.setItem(22, members);

        ItemStack visitors = new ItemStack(Material.RED_CONCRETE);
        ItemMeta visitorsMeta = visitors.getItemMeta();
        visitorsMeta.displayName(Component.text("Allow visitors").color(NamedTextColor.WHITE));
        if (allowVisitors) {
            visitors = new ItemStack(Material.GREEN_CONCRETE);
            visitorsMeta.lore(java.util.Arrays.asList(
                Component.text("On").color(NamedTextColor.GREEN),
                Component.text("Click to toggle access to visitors.").color(NamedTextColor.GRAY)
                ));
        } else {
            visitors = new ItemStack(Material.RED_CONCRETE);
            visitorsMeta.lore(java.util.Arrays.asList(
                Component.text("Off").color(NamedTextColor.RED),
                Component.text("Click to toggle access to visitors.").color(NamedTextColor.GRAY)
                ));
        }
        visitors.setItemMeta(visitorsMeta);
        islandGUI.setItem(4, visitors);

        ItemStack delete = new ItemStack(Material.BARRIER);
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.displayName(Component.text("Delete island").color(NamedTextColor.RED));
        deleteMeta.lore(java.util.Collections.singletonList(Component.text("Click to delete your island.").color(NamedTextColor.GRAY)));
        delete.setItemMeta(deleteMeta);
        islandGUI.setItem(15, delete);

        player.openInventory(islandGUI);
    }  

    private void membersGUI(Player player) {
        Inventory membersGUI = Bukkit.createInventory(player, 27, Component.text("Manage island members"));

        ItemStack players = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playersMeta = players.getItemMeta();
        playersMeta.displayName(Component.text("Player").color(NamedTextColor.YELLOW));
        players.setItemMeta(playersMeta);

        for (int i = 10; i < 17; i++) {
            membersGUI.setItem(i, players);
        }

        player.openInventory(membersGUI);
    }

    private void createIslandGUI(Player player) {
        Inventory createIslandGUI = Bukkit.createInventory(player, 27, Component.text("Create your island"));

        ItemStack create = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.displayName(Component.text("Create Island").color(NamedTextColor.GREEN));
        createMeta.lore(java.util.Collections.singletonList(Component.text("Click to create your island.").color(NamedTextColor.GRAY)));
        create.setItemMeta(createMeta);
        createIslandGUI.setItem(13, create);

        player.openInventory(createIslandGUI);
    }
}