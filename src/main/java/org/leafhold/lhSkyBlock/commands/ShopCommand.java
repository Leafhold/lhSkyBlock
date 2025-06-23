package org.leafhold.lhSkyBlock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.leafhold.lhSkyBlock.lhSkyBlock;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.npc.NPC;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;
import java.io.File;

public class ShopCommand implements CommandExecutor, Listener {
    private lhSkyBlock plugin;
    private DatabaseManager databaseManager;
    private FileConfiguration config;
    private NPCRegistry npcRegistry;

    public ShopCommand(lhSkyBlock plugin) {
        this.plugin = plugin;
        this.databaseManager = DatabaseManager.getInstance();
        try {
        this.databaseManager.connect();
        } catch (Exception e) {
        e.printStackTrace();
        }

        File configFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!configFile.exists()) {
            plugin.saveResource("shops.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.npcRegistry = CitizensAPI.getNPCRegistry();
    }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Please specify an NPC ID.").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("Please specify a shop name.").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    Integer npcId = Integer.parseInt(args[1]);
                    String shopName = args[2];
                    return createShop(player, npcId, shopName);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid NPC ID. Please provide a valid number.").color(NamedTextColor.RED));
                    return true;
                }
        }

        return false;
    }

    private boolean createShop(Player player, Integer npcId, String shopName) {
        if (config.contains("shops." + shopName)) {
            player.sendMessage(Component.text("A shop with this name already exists.").color(NamedTextColor.RED));
            return true;
        }

        NPC shopNPC = npcRegistry.getById(npcId);
        if (shopNPC == null) {
            player.sendMessage(Component.text("NPC with ID " + npcId + " does not exist.").color(NamedTextColor.RED));
            return true;
        }

        config.set("shops." + shopName + ".npc", shopNPC.getId());
        try {
            config.save(new File(plugin.getDataFolder(), "shops.yml"));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to save shop configuration.").color(NamedTextColor.RED));
            e.printStackTrace();
            return true;
        }

        return true;
    }

    private void openShop(Player player, String shopName) {
        if (!config.contains("shops." + shopName)) {
            player.sendMessage(Component.text("This shop does not exist.").color(NamedTextColor.RED));
            return;
            }
        if (!config.contains("shops." + shopName + ".items")) {
            player.sendMessage(Component.text("This shop has no items configured.").color(NamedTextColor.RED));
            return;
        }

        Inventory shopInventory = Bukkit.createInventory(player, 54, Component.text(shopName));
        for (String itemKey : config.getConfigurationSection("shops." + shopName + ".items").getKeys(false)) {
            ItemStack item = config.getItemStack("shops." + shopName + ".items." + itemKey);
            Integer slot = config.getInt("shops." + shopName + ".items." + itemKey + ".slot", -1);
            Integer defaultAmount = config.getInt("shops." + shopName + ".items." + itemKey + ".default_amount", 1);
            Double sellPrice = config.getDouble("shops." + shopName + ".items." + itemKey + ".sell.price");
            Double buyPrice = config.getDouble("shops." + shopName + ".items." + itemKey + ".buy.price");
            Integer minSellAmount = config.getInt("shops." + shopName + ".items." + itemKey + ".sell.min_amount", 1);
            Integer minBuyAmount = config.getInt("shops." + shopName + ".items." + itemKey + ".buy.min_amount", 1);
            
            if (item != null && slot >= 0 && slot < shopInventory.getSize()) {
                item.setAmount(defaultAmount);
                ItemMeta meta = item.getItemMeta();
                meta.lore(java.util.Collections.singletonList(Component.text("Sell price: $" + sellPrice).color(NamedTextColor.GREEN)
                    .append(Component.text("Buy price: $" + buyPrice).color(NamedTextColor.RED))
                ));
                item.setItemMeta(meta);
                shopInventory.setItem(slot, item);
            }
        }
        player.openInventory(shopInventory);
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        if (entity != null && npcRegistry.isNPC(entity)) {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            if (npc != null && npc.getId() == config.getInt("shops." + npc.getName() + ".npc")) {
                event.setCancelled(true);
                
                String shopName = npc.getName();
                openShop(player, shopName);
            }
        }
    }
}