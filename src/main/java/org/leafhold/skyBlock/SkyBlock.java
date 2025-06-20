package org.leafhold.skyBlock;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.leafhold.skyBlock.commands.IslandCommand;
import org.leafhold.skyBlock.utils.DatabaseManager;

public final class SkyBlock extends JavaPlugin {
    private static SkyBlock instance;
    private static boolean isSpigot;
    private static boolean isPaper;

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

        try {
            DatabaseManager.getInstance().connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to database: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        IslandCommand islandCommand = new IslandCommand();
        getCommand("island").setExecutor(islandCommand);
        getServer().getPluginManager().registerEvents(islandCommand, instance);
    }

    @Override
    public void onDisable() {
        DatabaseManager.getInstance().disconnect();
    }

    public static SkyBlock getInstance() {
        return instance;
    }
}
