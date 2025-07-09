package org.leafhold.lhSkyBlock.commands;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.leafhold.lhSkyBlock.crates.KeysHolder;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KeysCommand implements CommandExecutor, Listener, TabCompleter {
    private lhSkyBlock plugin;
    private DatabaseManager databaseManager;
    private FileConfiguration config;
    private NPCRegistry npcRegistry;

    public KeysCommand(lhSkyBlock plugin) {
        this.plugin = plugin;
        databaseManager = DatabaseManager.getInstance();
        npcRegistry = CitizensAPI.getNPCRegistry();

        File configFile = new File(plugin.getDataFolder(), "keys.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if (args.length > 1) {
            switch (args[0].toLowerCase()) {
                case "setup":
                    if (args.length < 2) {
                        player.sendMessage(Component.text("Usage: /keys setup [crate|npc]").color(NamedTextColor.RED));
                        return true;
                    }
                    switch (args[1].toLowerCase()) {
                        case "npc":
                            if (args.length < 3) {
                                player.sendMessage(Component.text("Please specify an NPC ID.").color(NamedTextColor.RED));
                                return true;
                            }
                            try {
                                int npcId = Integer.parseInt(args[2]);
                                NPC npc = npcRegistry.getById(npcId);
                                if (npc == null) {
                                    player.sendMessage(Component.text("NPC with ID " + npcId + " not found.").color(NamedTextColor.RED));
                                    return true;
                                }
                                return createKeyNPC(player, npc);
                            } catch (NumberFormatException e) {
                                player.sendMessage(Component.text("Invalid NPC ID: " + args[2]).color(NamedTextColor.RED));
                                return true;
                            }
                        case "crate":
                            if (args.length < 3) {
                                player.sendMessage(Component.text("Please specify a crate name.").color(NamedTextColor.RED));
                                return true;
                            }
                            if (args.length < 6) {
                                player.sendMessage(Component.text("Please specify the key location").color(NamedTextColor.RED));
                                return true;
                            }
                            String crateName = args[2];
                            if (crateName != "vote" && crateName != "bronze" && crateName != "silver" && crateName != "gold" && crateName != "diamond") {
                                player.sendMessage(Component.text("Invalid crate. Valid crates are: vote, bronze, silver, gold, diamond.").color(NamedTextColor.RED));
                                return true;
                            }
                            if (config.get("keys-crate." + crateName) != null) {
                                player.sendMessage(Component.text("This crate was already set up.").color(NamedTextColor.RED));
                                return true;
                            }
                            config.set("keys-crate." + crateName + ".world", player.getWorld().getName());
                            config.set("keys-crate." + crateName + ".x", args[3]);
                            config.set("keys-crate." + crateName + ".y", args[4]);
                            config.set("keys-crate." + crateName + ".z", args[5]);
                            try {
                                config.save(new File(plugin.getDataFolder(), "keys.yml"));
                                player.sendMessage(Component.text("Keys crate " + crateName + " set up successfully!").color(NamedTextColor.GREEN));
                            } catch (Exception e) {
                                player.sendMessage(Component.text("Failed to save keys crate configuration.").color(NamedTextColor.RED));
                                e.printStackTrace();
                            }
                            return true;
                        }
                default:
                    return false;
            }
        }
        return true;
    }

    private boolean createKeyNPC(Player player, NPC npc) {
        if (config.get("keys-npc") != null) {
            player.sendMessage(Component.text("This NPC already has keys set up.").color(NamedTextColor.RED));
            return true;
        }
        config.set("keys-npc", npc.getId());
        try {
            config.save(new File(plugin.getDataFolder(), "keys.yml"));
            player.sendMessage(Component.text("Keys NPC set up successfully!").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to save keys NPC configuration.").color(NamedTextColor.RED));
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("setup");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setup":
                    completions.add("npc");
                    completions.add("crate");
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("npc")) {
            for (NPC npc : npcRegistry) {
                completions.add(String.valueOf(npc.getId()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("crate")) {
            completions.add("vote");
            completions.add("bronze");
            completions.add("silver");
            completions.add("gold");
            completions.add("diamond");
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("crate")) {
            Block targetBlock = ((Player) sender).getTargetBlock(null, 10);
            if (targetBlock == null) return completions;
            if (args.length == 4) completions.add(String.valueOf(targetBlock.getX()));
            else if (args.length == 5) completions.add(String.valueOf(targetBlock.getY()));
            else if (args.length == 6) completions.add(String.valueOf(targetBlock.getZ()));
        }
        return completions;
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity != null && npcRegistry.isNPC(entity)) {
            NPC npc = npcRegistry.getNPC(entity);
            if (npc != null && config.getInt("keys-npc", -1) == npc.getId()) {
                event.setCancelled(true);
                Inventory keysInventory = Bukkit.createInventory(new KeysHolder(), 54, Component.text("Keys"));
                List<ItemStack> keys = databaseManager.getKeysForPlayer(player.getUniqueId());
                if (keys != null && !keys.isEmpty()) {
                    for (ItemStack key : keys) {
                        if (key != null) {
                            keysInventory.addItem(key);
                        }
                    }
                }
                player.openInventory(keysInventory);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTopInventory().getHolder() instanceof KeysHolder) {
            if (event.getClickedInventory() == null) return;
            if (event.getClickedInventory().getHolder() instanceof Player) {
                event.setCancelled(true);
                return;
            }
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                ItemStack clickedItem = event.getCurrentItem();
                String keyType = clickedItem.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING);
                if (keyType != null) {
                    event.setCancelled(true);
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage(Component.text("You do not have enough space in your inventory.").color(NamedTextColor.RED));
                        return;
                    }
                    boolean removed = databaseManager.removeKey(player.getUniqueId(), clickedItem);
                    if (removed) {
                        event.getClickedInventory().removeItem(clickedItem);
                        player.getInventory().addItem(clickedItem);
                    } else {
                        player.sendMessage(Component.text("There was an error adding the key to your inventory.").color(NamedTextColor.RED));
                        return;
                    }
                } else {
                    player.sendMessage(Component.text("This item is not a valid key.").color(NamedTextColor.RED));
                    event.setCancelled(true);
                }
            }
        }
    }
}
