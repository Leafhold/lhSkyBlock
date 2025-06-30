package org.leafhold.lhSkyBlock.commands;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.islands.IslandMenuHolder;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.leafhold.lhSkyBlock.islands.IslandSpawning;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.command.TabCompleter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.SQLException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

public class IslandCommand implements CommandExecutor, Listener, TabCompleter {
    private lhSkyBlock plugin;
    private FileConfiguration config;
    private DatabaseManager databaseManager;
    private final Map<UUID, Long> visitorToggleCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000;
    private static IslandSpawning islandSpawning;

    public IslandCommand(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        databaseManager = DatabaseManager.getInstance();
        islandSpawning = new IslandSpawning(plugin);
    }

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
        List<Object> userIslands = new ArrayList<>();
        try {
            userIslands = databaseManager.getIslandsByOwner(uuid.toString());
        } catch (SQLException e) {
            player.sendMessage(Component.text("An error occurred while fetching your islands. Please try again later.").color(NamedTextColor.RED));
            e.printStackTrace();
            return true;
        }
        if (userIslands == null) {
            player.sendMessage(Component.text("Could not fetch your islands. Please try again later.").color(NamedTextColor.RED));
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
                    selectIslandGUI(player);
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
                    deleteIslandGUI(player, uuid);
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
                    Object island = userIslands.get(0);
                    Object[] islandObj = (Object[]) island;
                    UUID islandUUID = UUID.fromString(islandObj[0].toString());
                    teleportToIsland(player, islandUUID);
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
                return false;
        }
        return true;
    }

    private void teleportToIsland(Player player, UUID islandUUID) {
        if (islandUUID == null) {
            player.sendMessage(Component.text("Island UUID is null.").color(NamedTextColor.RED));
            return;
        }
        Object islandData = null;
        try {
            islandData = databaseManager.getIslandByUUID(islandUUID);
        } catch (SQLException e) {
            player.sendMessage(Component.text("An error occurred while fetching the island. Please try again later.").color(NamedTextColor.RED));
            e.printStackTrace();
            return;
        }
        if (islandData == null) {
            player.sendMessage(Component.text("Island not found.").color(NamedTextColor.RED));
            return;
        }
        Object[] islandObj = (Object[]) islandData;
        Integer islandIndex = (Integer) islandObj[4];
        if (islandIndex < 0) {
            player.sendMessage(Component.text("Invalid island index.").color(NamedTextColor.RED));
            return;
        }

        World islandWorld = IslandSpawning.loadWorld();
        Location islandLocation = IslandSpawning.getIslandSpawnLocation(islandIndex, islandWorld);
        if (islandLocation == null) {
            player.sendMessage(Component.text("Island location not found.").color(NamedTextColor.RED));
            return;
        }
        islandLocation.setPitch(0);
        islandLocation.setYaw(180);
        islandLocation.add(0,1,0);
        player.teleportAsync(islandLocation);
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
            .append(Component.text(" - Delete your island."))
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            completions.add("create");
            completions.add("home");
            completions.add("delete");
        }

        return completions;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTopInventory().getHolder() instanceof IslandMenuHolder) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof IslandMenuHolder) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    ItemStack item = event.getCurrentItem();
                    ItemMeta meta = item.getItemMeta();
                    String itemRole = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "item_role"), PersistentDataType.STRING);
                    String itemKey = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "item_key"), PersistentDataType.STRING);
                    String islandUUIDString = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "island_uuid"), PersistentDataType.STRING);
                    UUID islandUUID = null;
                    if (islandUUIDString != null && !islandUUIDString.isEmpty()) {
                        islandUUID = UUID.fromString(islandUUIDString);
                    }
                    if (itemRole != null) {
                        switch (itemRole) {
                            case "manage_island":
                                if (islandUUID == null) {
                                    player.sendMessage(Component.text("Island UUID not found.").color(NamedTextColor.RED));
                                    return;
                                }
                                switch (itemKey) {
                                    case "island_home":
                                        player.sendMessage(Component.text("Teleporting to your island...").color(NamedTextColor.AQUA));
                                        teleportToIsland(player, islandUUID);
                                        break;
                                    case "allow_visitors":
                                        long currentTime = System.currentTimeMillis();
                                        if (visitorToggleCooldown.containsKey(player.getUniqueId())) {
                                            long lastUsed = visitorToggleCooldown.get(player.getUniqueId());
                                            if (currentTime - lastUsed < COOLDOWN_TIME) {
                                                long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
                                                player.sendMessage(Component.text("Please wait " + remainingTime + " seconds before toggling again.")
                                                    .color(NamedTextColor.RED));
                                                return;
                                            }
                                        }
                                        
                                        visitorToggleCooldown.put(player.getUniqueId(), currentTime);
                                        
                                        try {
                                            boolean currentVisitorState = databaseManager.visitorsAllowed(islandUUID.toString());
                                            databaseManager.toggleVisitors(islandUUID);
                                            
                                            String message = currentVisitorState ? 
                                                "Visitors are no longer allowed on your island." : 
                                                "Visitors are now allowed on your island.";
                                            
                                            player.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
                                            
                                            ItemStack updatedVisitors;
                                            ItemMeta updatedVisitorsMeta;
                                            
                                            if (currentVisitorState) {
                                                updatedVisitors = new ItemStack(Material.RED_CONCRETE);
                                                updatedVisitorsMeta = updatedVisitors.getItemMeta();
                                                updatedVisitorsMeta.displayName(Component.text("Allow visitors").color(NamedTextColor.WHITE));
                                                updatedVisitorsMeta.lore(java.util.Arrays.asList(
                                                    Component.text("Off").color(NamedTextColor.RED),
                                                    Component.text("Click to toggle access to visitors.").color(NamedTextColor.GRAY)
                                                ));
                                            } else {
                                                updatedVisitors = new ItemStack(Material.GREEN_CONCRETE);
                                                updatedVisitorsMeta = updatedVisitors.getItemMeta();
                                                updatedVisitorsMeta.displayName(Component.text("Allow visitors").color(NamedTextColor.WHITE));
                                                updatedVisitorsMeta.lore(java.util.Arrays.asList(
                                                    Component.text("On").color(NamedTextColor.GREEN),
                                                    Component.text("Click to toggle access to visitors.").color(NamedTextColor.GRAY)
                                                ));
                                            }
                                            
                                            updatedVisitorsMeta.getPersistentDataContainer().set(
                                                new org.bukkit.NamespacedKey(plugin, "item_role"),
                                                PersistentDataType.STRING,
                                                "manage_island"
                                            );
                                            updatedVisitorsMeta.getPersistentDataContainer().set(
                                                new org.bukkit.NamespacedKey(plugin, "item_key"),
                                                PersistentDataType.STRING,
                                                "allow_visitors"
                                            );
                                            updatedVisitorsMeta.getPersistentDataContainer().set(
                                                new org.bukkit.NamespacedKey(plugin, "island_uuid"),
                                                PersistentDataType.STRING,
                                                islandUUID.toString()
                                            );
                                            
                                            updatedVisitors.setItemMeta(updatedVisitorsMeta);
                                            
                                            event.getInventory().setItem(15, updatedVisitors);
                                            
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            player.sendMessage(Component.text("An error occurred while updating island visitors.").color(NamedTextColor.RED));
                                        }
                                        break;
                                    case "members":
                                        membersGUI(player, islandUUID);
                                        break;
                                }
                                break;
                            case "manage_members":
                                break;
                            case "create_island":
                                player.closeInventory();
                                player.sendMessage(Component.text("Creating a new island...").color(NamedTextColor.AQUA));
                                World islandWorld = Bukkit.getWorlds().stream()
                                    .filter(world -> world.getName().equalsIgnoreCase("islands"))
                                    .findFirst()
                                    .orElse(null);
                                if (islandWorld == null) {
                                    islandWorld = IslandSpawning.loadWorld();
                                }
                                Integer islandIndex;
                                Object[] result = databaseManager.createIsland(
                                    player.getUniqueId(),
                                    player.getName() + "'s Island",
                                    islandWorld.getName()
                                );
                                
                                if (result != null) {
                                    islandIndex = (Integer) result[1];
                                    Location islandLocation = IslandSpawning.getIslandSpawnLocation(islandIndex, islandWorld);
                                    double x = islandLocation.getX();
                                    double z = islandLocation.getZ();

                                    String schematicName = config.getString("islands.default-island.schematic");
                                    if (schematicName == null || schematicName.isEmpty()) {
                                        player.sendMessage(Component.text("Island schematic not found in config.").color(NamedTextColor.RED));
                                        return;
                                    }
                                    File schematicFile = new File(plugin.getDataFolder(), "schematics/" + schematicName);
                                    boolean pasted = islandSpawning.pasteIsland(
                                        schematicFile,
                                        islandLocation,
                                        player
                                    );

                                    if (!pasted) {
                                        player.sendMessage(Component.text("Failed to paste island schematic.").color(NamedTextColor.RED));
                                        return;
                                    }
                                    player.sendMessage(Component.text("Island created successfully!").color(NamedTextColor.GREEN));
                                    player.teleport(islandLocation.add(x > 0 ? 0.5 : -0.5, 0.0, z > 0 ? 0.5 : -0.5).setRotation(180, 0));
                                } else {
                                    player.sendMessage(Component.text("Failed to create island. You might already have one.").color(NamedTextColor.RED));
                                }
                                break;
                            case "select_island":
                                if (islandUUID == null) {
                                    player.sendMessage(Component.text("Island UUID not found.").color(NamedTextColor.RED));
                                    return;
                                }
                                manageIslandGUI(player, islandUUID);
                                break;
                            case "delete_island":
                                if (islandUUID == null) {
                                    player.sendMessage(Component.text("Island UUID not found.").color(NamedTextColor.RED));
                                    return;
                                }
                                switch (itemKey) {
                                    case "select_for_deletion":
                                        confirmDeleteIslandGUI(player, islandUUID);
                                        break;
                                }
                                break;
                            case "confirm_delete_island":
                                switch (itemKey) {
                                    case "confirm":
                                        if (islandUUID == null) {
                                            player.sendMessage(Component.text("Island UUID not found.").color(NamedTextColor.RED));
                                            return;
                                        }
                                        Object islandData = databaseManager.getIslandByUUID(islandUUID);
                                        if (islandData == null) {
                                            player.sendMessage(Component.text("Island not found.").color(NamedTextColor.RED));
                                            return;
                                        }
                                        if (!((Object[]) islandData)[1].toString().equals(player.getUniqueId().toString())) {
                                            player.sendMessage(Component.text("You do not own this island.").color(NamedTextColor.RED));
                                            return;
                                        }
                                        Location islandLocation = IslandSpawning.getIslandSpawnLocation(
                                            (Integer) ((Object[]) islandData)[4],
                                            Bukkit.getWorld("islands")
                                        );
                                        player.sendMessage(Component.text("Deleting island...").color(NamedTextColor.AQUA));
                                        if (player.getLocation().getWorld().getName().equals("islands")) {
                                            if (IslandSpawning.getIslandIndexFromLocation(player.getLocation()) == ((Object[]) islandData)[4]) {
                                                Location spawnLocation = Bukkit.getWorld("world").getSpawnLocation();
                                                Double x = spawnLocation.getX();
                                                Double z = spawnLocation.getZ();
                                                player.teleportAsync(spawnLocation.add(x > 0 ? 0.5 : -0.5, 0.0, z > 0 ? 0.5 : -0.5).setRotation(180, 0));
                                            }
                                        };
                                        Boolean deleted = IslandSpawning.deleteIsland(islandLocation);
                                        if (deleted == null || !deleted) {
                                            player.sendMessage(Component.text("Failed to delete island. Please try again later.").color(NamedTextColor.RED));
                                            return;
                                        }

                                        deleted = databaseManager.deleteIsland(player, islandUUID.toString());
                                        if (deleted) {
                                            player.sendMessage(Component.text("Island deleted successfully.").color(NamedTextColor.GREEN));
                                        } else {
                                            player.sendMessage(Component.text("Failed to delete island. You might not own this island.").color(NamedTextColor.RED));
                                        }
                                        break;
                                    case "cancel":
                                        player.sendMessage(Component.text("Island deletion cancelled.").color(NamedTextColor.YELLOW));
                                        break;
                                }
                                player.closeInventory();
                                break;
                        }
                    }
                }
            }
        }
    }

    private void selectIslandGUI(Player player) {
        List<Object> userIslands = new ArrayList<>();
        try {
            userIslands = databaseManager.getIslandsByOwner(player.getUniqueId().toString());
        } catch (SQLException e) {
            player.sendMessage(Component.text("An error occurred while fetching your islands. Please try again later.").color(NamedTextColor.RED));
            e.printStackTrace();
            return;
        }
        Inventory selectIslandGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Select island"));

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
                new org.bukkit.NamespacedKey(plugin, "island_uuid"),
                PersistentDataType.STRING,
                islandUUID.toString()
            );
            islandItem.setItemMeta(islandMeta);
            islandItems.add(islandItem);
        }

        for (int i = 0; i < userIslands.size(); i++) {
            selectIslandGUI.setItem(i, islandItems.get(i));
        }

        player.openInventory(selectIslandGUI);   
    }

    private void manageIslandGUI(Player player, UUID islandUUID) {
        Object island = null;
        try {
            island = databaseManager.getIslandByUUID(islandUUID);
        } catch (SQLException e) {
            player.sendMessage(Component.text("An error occurred while fetching your islands. Please try again later.").color(NamedTextColor.RED));
            e.printStackTrace();
            return;
        }
        Object[] islandObj = (Object[]) island;
        boolean allowVisitors = Boolean.parseBoolean(islandObj[3].toString());

        Inventory islandGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Manage island"));

        ItemStack home = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta homeMeta = home.getItemMeta();
        homeMeta.displayName(Component.text("Island home").color(NamedTextColor.AQUA));
        homeMeta.lore(java.util.Collections.singletonList(Component.text("Click to teleport to your island.").color(NamedTextColor.GRAY)));
        homeMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "manage_island"
        );
        homeMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_key"),
            PersistentDataType.STRING,
            "island_home"
        );
        homeMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "island_uuid"),
            PersistentDataType.STRING,
            islandUUID.toString()
        );
        home.setItemMeta(homeMeta);
        islandGUI.setItem(11, home);

        ItemStack members = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta membersMeta = members.getItemMeta();
        membersMeta.displayName(Component.text("Members").color(NamedTextColor.WHITE));
        membersMeta.lore(java.util.Collections.singletonList(Component.text("Click to view your island members.").color(NamedTextColor.GRAY)));
        membersMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "manage_island"
        );
        membersMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_key"),
            PersistentDataType.STRING,
            "members"
        );
        membersMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "island_uuid"),
            PersistentDataType.STRING,
            islandUUID.toString()
        );
        members.setItemMeta(membersMeta);
        islandGUI.setItem(13, members);

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
        }        visitorsMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "manage_island"
        );
        visitorsMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_key"),
            PersistentDataType.STRING,
            "allow_visitors"
        );
        visitorsMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "island_uuid"),
            PersistentDataType.STRING,
            islandUUID.toString()
        );
        visitors.setItemMeta(visitorsMeta);
        islandGUI.setItem(15, visitors);

        player.openInventory(islandGUI);
    }  

    private void membersGUI(Player player, UUID islandUUID) {
        Inventory membersGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Manage island members"));

        //todo fetch island members

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
        Inventory createIslandGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Create an island"));

        ItemStack create = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.displayName(Component.text("Create Island").color(NamedTextColor.GREEN));
        createMeta.lore(java.util.Collections.singletonList(Component.text("Click to create your island.").color(NamedTextColor.GRAY)));
        createMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "create_island"
        );
        create.setItemMeta(createMeta);
        createIslandGUI.setItem(13, create);

        player.openInventory(createIslandGUI);
    }

    private void deleteIslandGUI(Player player, UUID islandUUID) {
        Inventory deleteIslandGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Delete an island"));

        List<Object> userIslands = new ArrayList<>();
        try {
            userIslands = databaseManager.getIslandsByOwner(player.getUniqueId().toString());
        } catch (SQLException e) {
            player.sendMessage(Component.text("An error occurred while fetching your islands. Please try again later.").color(NamedTextColor.RED));
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < userIslands.size(); i++) {
            Object island = userIslands.get(i);
            Object[] islandObj = (Object[]) island;
            UUID currentIslandUUID = UUID.fromString(islandObj[0].toString());
            String islandName = islandObj[2].toString();

            ItemStack islandItem = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta islandMeta = islandItem.getItemMeta();
            islandMeta.displayName(Component.text(islandName).color(NamedTextColor.GREEN));
            islandMeta.lore(java.util.Collections.singletonList(Component.text("Click to delete this island.").color(NamedTextColor.DARK_RED)));
            islandMeta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "island_uuid"),
                PersistentDataType.STRING,
                currentIslandUUID.toString()
            );
            islandMeta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "item_role"),
                PersistentDataType.STRING,
                "delete_island"
            );
            islandMeta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "item_key"),
                PersistentDataType.STRING,
                "select_for_deletion"
            );
            islandItem.setItemMeta(islandMeta);
            deleteIslandGUI.setItem(i, islandItem);
        }

        player.openInventory(deleteIslandGUI);
    }

   private void confirmDeleteIslandGUI(Player player, UUID islandUUID) {
        Inventory confirmDeleteGUI = Bukkit.createInventory(new IslandMenuHolder(), 27, Component.text("Confirm island deletion"));

        ItemStack confirm = new ItemStack(Material.RED_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm").color(NamedTextColor.RED));
        confirmMeta.lore(java.util.Collections.singletonList(Component.text("Click to delete this island.").color(NamedTextColor.DARK_RED)));
        confirmMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "island_uuid"),
            PersistentDataType.STRING,
            islandUUID.toString()
        );
        confirmMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "confirm_delete_island"
        );
        confirmMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_key"),
            PersistentDataType.STRING,
            "confirm"
        );
        confirm.setItemMeta(confirmMeta);

        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel").color(NamedTextColor.YELLOW));
        cancelMeta.lore(java.util.Collections.singletonList(Component.text("Click to cancel deletion.").color(NamedTextColor.GRAY)));
        cancelMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_role"),
            PersistentDataType.STRING,
            "confirm_delete_island"
        );
        cancelMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "item_key"),
            PersistentDataType.STRING,
            "cancel"
        );
        cancel.setItemMeta(cancelMeta);

        confirmDeleteGUI.setItem(12, confirm);
        confirmDeleteGUI.setItem(14, cancel);

        player.openInventory(confirmDeleteGUI);
    }
}