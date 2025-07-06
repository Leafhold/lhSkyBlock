package org.leafhold.lhSkyBlock.commands;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.leafhold.lhSkyBlock.crates.KeysHolder;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            try {
                int npcId = Integer.parseInt(args[1]);
                NPC npc = npcRegistry.getById(npcId);
                if (npc == null) {
                    player.sendMessage(Component.text("NPC with ID " + npcId + " not found.").color(NamedTextColor.RED));
                    return true;
                }
                createKeyNPC(player, npc);
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            for (NPC npc : npcRegistry) {
                completions.add(String.valueOf(npc.getId()));
            }
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
}
