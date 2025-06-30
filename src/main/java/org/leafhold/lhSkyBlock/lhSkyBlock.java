package org.leafhold.lhSkyBlock;

import org.leafhold.lhSkyBlock.commands.IslandCommand;
import org.leafhold.lhSkyBlock.listeners.PlayerJoinListener;
import org.leafhold.lhSkyBlock.listeners.PlayerLeaveListener;
import org.leafhold.lhSkyBlock.listeners.PlayerDeathListener;
import org.leafhold.lhSkyBlock.listeners.VoidTeleportListener;
import org.leafhold.lhSkyBlock.shops.SignShop;
import org.leafhold.lhSkyBlock.commands.ShopCommand;
import org.leafhold.lhSkyBlock.commands.VisitCommand;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.leafhold.lhSkyBlock.utils.VoidWorldGenerator;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import java.nio.file.Files;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

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

        this.loadIslandsWorld();

        IslandCommand islandCommand = new IslandCommand(instance); // pass databaseManager here
        getCommand("island").setExecutor(islandCommand);
        getCommand("island").setTabCompleter(islandCommand);
        getServer().getPluginManager().registerEvents(islandCommand, instance);

        VisitCommand visitCommand = new VisitCommand(instance);
        getCommand("visit").setExecutor(visitCommand);
      
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

    public boolean isPaper() {
        return isPaper;
    }

    public static lhSkyBlock getInstance() {
        return instance;
    }

    public World loadIslandsWorld() {
        World islandWorld = Bukkit.getWorld("islands");
        if (islandWorld != null) {
            return islandWorld;
        }
        boolean doesWorldExist = Files.exists(Bukkit.getWorldContainer().toPath().resolve("islands"));
        if (doesWorldExist) {
            islandWorld = Bukkit.createWorld(new WorldCreator("islands"));
            return islandWorld;
        }
        WorldCreator creator = new WorldCreator("islands");
        creator.generator(new VoidWorldGenerator());
        islandWorld = creator.createWorld();
        islandWorld.setDifficulty(org.bukkit.Difficulty.NORMAL);
        islandWorld.setPVP(false);
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = regionContainer.get(BukkitAdapter.adapt(islandWorld));
        if (regions != null && regions.hasRegion("__global__")) {
            ProtectedRegion globalRegion = regions.getRegion("__global__");
            if (globalRegion != null) {
                globalRegion.setFlag(Flags.BUILD, State.DENY);
                globalRegion.setFlag(Flags.CHEST_ACCESS, State.DENY);
                globalRegion.setFlag(Flags.CROP_GROWTH, State.DENY);
                globalRegion.setFlag(Flags.OTHER_EXPLOSION, State.DENY);
                globalRegion.setFlag(Flags.BLOCK_PLACE, State.DENY);
                globalRegion.setFlag(Flags.BLOCK_BREAK, State.DENY);
                globalRegion.setFlag(Flags.PVP, State.DENY);
                globalRegion.setFlag(Flags.MOB_SPAWNING, State.DENY);
                globalRegion.setFlag(Flags.CREEPER_EXPLOSION, State.DENY);
                globalRegion.setFlag(Flags.ENDER_BUILD, State.DENY);
                globalRegion.setFlag(Flags.TNT, State.DENY);
                globalRegion.setFlag(Flags.FIRE_SPREAD, State.DENY);
                globalRegion.setFlag(Flags.LIGHTER, State.DENY);
                globalRegion.setFlag(Flags.GHAST_FIREBALL, State.DENY);
                try {
                    regions.save();
                } catch (StorageException e) {
                    instance.getLogger().severe("Failed to save WorldGuard regions: " + e.getMessage());
                }
            }
        }
        return islandWorld;
    }
}
