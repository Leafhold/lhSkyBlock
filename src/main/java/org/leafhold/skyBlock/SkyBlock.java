package org.leafhold.skyBlock;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.leafhold.skyBlock.commands.IslandCommand;

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
        getCommand("island").setExecutor(new IslandCommand());
        getServer().getPluginManager().registerEvents(new IslandCommand(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static SkyBlock getInstance() {
        return instance;
    }
}
