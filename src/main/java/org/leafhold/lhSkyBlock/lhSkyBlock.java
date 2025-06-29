package org.leafhold.lhSkyBlock;

import javax.xml.crypto.Data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.TabCompleter;

import org.leafhold.lhSkyBlock.commands.IslandCommand;
import org.leafhold.lhSkyBlock.listeners.PlayerJoinListener;
import org.leafhold.lhSkyBlock.listeners.PlayerLeaveListener;
import org.leafhold.lhSkyBlock.listeners.PlayerDeathListener;
import org.leafhold.lhSkyBlock.listeners.VoidTeleportListener;
import org.leafhold.lhSkyBlock.shops.SignShop;
import org.leafhold.lhSkyBlock.commands.ShopCommand;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;

public final class lhSkyBlock extends JavaPlugin {
    private static lhSkyBlock instance;
    private static boolean isSpigot;
    private static boolean isPaper;
    private static DatabaseManager databaseManager;

    //todo add paper specific code

    @Override
    public void onLoad() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            isSpigot = true;
        } catch (ClassNotFoundException e) {
            this.getLogger().severe("This plugin requires Spigot or Paper to run!");
            isSpigot = false;
            return;
        }
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            isPaper = true;
        } catch (ClassNotFoundException e) {
            isPaper = false;
        }
    }

    @Override
    public void onEnable() {
        if (!isSpigot) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        saveDefaultConfig();
        databaseManager = new DatabaseManager(instance);
        try {
            databaseManager.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to the database: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        IslandCommand islandCommand = new IslandCommand(instance); // pass databaseManager here
        getCommand("island").setExecutor(islandCommand);
        getCommand("island").setTabCompleter(islandCommand);
        getServer().getPluginManager().registerEvents(islandCommand, instance);
      
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(instance), instance);
        getServer().getPluginManager().registerEvents(new PlayerLeaveListener(instance), instance);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(instance), instance);
        getServer().getPluginManager().registerEvents(new VoidTeleportListener(instance), instance);

        if (Bukkit.getPluginManager().isPluginEnabled("FancyHolograms")) {
            getLogger().info("FancyHolograms found - enabling sign shops");
            getServer().getPluginManager().registerEvents(new SignShop(instance), instance);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            ShopCommand shopCommand = new ShopCommand(instance);
            getCommand("shop").setExecutor(shopCommand);
            getCommand("shop").setTabCompleter(shopCommand);
            getServer().getPluginManager().registerEvents(shopCommand, instance);
        } else {
            getLogger().warning("Citizens not found - shop command disabled");
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            try {
                databaseManager.disconnect();
            } catch (Exception e) {
                getLogger().severe("Failed to disconnect from the database: " + e.getMessage());
            }
        }
    }

    public static boolean isPaper() {
        return isPaper;
    }

    public static lhSkyBlock getInstance() {
        return instance;
    }
}
