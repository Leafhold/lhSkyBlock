package org.leafhold.lhSkyBlock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.EntityType;

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
            plugin.saveResource(plugin.getDataFolder() + "/shops.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
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
            //todo error message
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Please specify a shop name.").color(NamedTextColor.RED));
                    return true;
                }
                createShop(player, args[1]);
        }

        return false;
    }

    private void createShop(Player player, String shopName) {
        if (config.contains("shops." + shopName)) {
            player.sendMessage(Component.text("A shop with this name already exists.").color(NamedTextColor.RED));
            return;
        }

        NPCRegistry npcRegistry = CitizensAPI.getNPCRegistry();
        NPC shopNPC = npcRegistry.createNPC(EntityType.VILLAGER, shopName);
        config.set("shops." + shopName + ".npc", shopNPC.getId());
    }
}
