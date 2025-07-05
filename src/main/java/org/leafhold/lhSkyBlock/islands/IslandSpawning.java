package org.leafhold.lhSkyBlock.islands;

import org.bukkit.configuration.file.FileConfiguration;
import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.utils.VoidWorldGenerator;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag.State;

import com.fastasyncworldedit.core.util.TaskManager;

import com.fastasyncworldedit.core.util.TaskManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class IslandSpawning {
    private static lhSkyBlock plugin;
    private static FileConfiguration config;
    private static Integer islandSpawnY;
    private static Integer islandSpacing;
    private static Map<Integer, Location> islandLocations = new HashMap<>();
    
    public IslandSpawning(lhSkyBlock plugin) {
        IslandSpawning.plugin = plugin;
        config = plugin.getConfig();
        islandSpawnY = config.getInt("islands.y-coordinate");
        islandSpacing = config.getInt("islands.spacing");
    }
    
    public boolean pasteIsland(File schematicFile, Location location, Player owner) {
        if (schematicFile == null || !schematicFile.exists()) {
            plugin.getLogger().severe("Schematic file does not exist");
            return false;
        }

        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().severe("Unsupported schematic format");
            return false;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read schematic file: " + e.getMessage());
            return false;
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .ignoreAirBlocks(false)
                .build();
            Operations.complete(operation);
            editSession.flushQueue();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste island schematic: " + e.getMessage());
            return false;
        }
        //todo Add worldguard region creation and ownership assignment
    }

    public static Location getIslandSpawnLocation(Integer islandIndex, World world) {
        if (world == null || islandIndex < 0) {
            plugin.getLogger().severe("Invalid world or island index");
            return null;
        }
        if (islandLocations.containsKey(islandIndex)) {
            return islandLocations.get(islandIndex);
        } else {
            if (islandIndex == 0) {
                return new Location(world, 0.5, islandSpawnY, 0.5);
            }

            Integer layer = 1;
            Integer previousLayerIslands = 0;

            while (true) {
                Integer layerIslands = 8 * layer;
                if (islandIndex <= previousLayerIslands + layerIslands) break;
                previousLayerIslands += layerIslands;
                layer++;
            }

            Integer layerIndex = islandIndex - previousLayerIslands - 1;
            Integer x, z;
            
            if (layerIndex < 2 * layer) {
                x = layer;
                z = -layer + layerIndex;
            } else if (layerIndex < 4 * layer) {
                x = layer - (layerIndex - 2 * layer);
                z = layer;
            } else if (layerIndex < 6 * layer) {
                x = -layer;
                z = layer - (layerIndex - 4 * layer);
            } else {
                x = -layer + (layerIndex - 6 * layer);
                z = -layer;
            }
            islandLocations.put(islandIndex, new Location(world, x, islandSpawnY, z));
            return new Location(world, x * islandSpacing, islandSpawnY, z * islandSpacing);
        }
    }

    public static Integer getIslandIndexFromLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().severe("Invalid location");
            return null;
        }
        World world = location.getWorld();
        for (Integer index = 0; index < 10000; index++) { //todo Replace 10,000 with configurable max islands per world
            Location island = getIslandSpawnLocation(index, world);
            if (island == null) {
                plugin.getLogger().severe("Failed to get island spawn location for index: " + index);
                return null;
            }
            Integer distanceX = Math.abs(location.getBlockX() - island.getBlockX());
            Integer distanceZ = Math.abs(location.getBlockZ() - island.getBlockZ());
            Integer islandSpacing = config.getInt("islands.spacing");
            if (distanceX <= islandSpacing / 2 && distanceZ <= islandSpacing / 2) {
                return index;
            }
        }
        return null;
    }

    public static boolean deleteIsland(Location location) {
        if (location == null || location.getWorld() == null) return false;
        Integer islandIndex = getIslandIndexFromLocation(location);
        Location islandLocation = getIslandSpawnLocation(islandIndex, Bukkit.getWorld("islands"));

        Integer minY = islandLocation.getWorld().getMinHeight();
        Integer maxY = islandLocation.getWorld().getMaxHeight() - 1;
        Location minLocation = islandLocation.clone();
        minLocation.setY(minY);
        minLocation.subtract(islandSpacing / 2, 0, islandSpacing / 2);
        Location maxLocation = islandLocation.clone();
        maxLocation.setY(maxY);
        maxLocation.add(islandSpacing / 2, 0, islandSpacing / 2);
        BlockVector3 min = BukkitAdapter.asBlockVector(minLocation);
        BlockVector3 max = BukkitAdapter.asBlockVector(maxLocation);
        Region region = new CuboidRegion(min, max);

        TaskManager.taskManager().async(() -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(islandLocation.getWorld()))
                    .build()) {
                editSession.setBlocks(region, BlockTypes.AIR.getDefaultState());
                editSession.flushQueue();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to delete island at location " + islandLocation + ": " + e.getMessage());
            }
        });
        return true;
    }

    public static World loadWorld() {
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
                    plugin.getLogger().severe("Failed to save WorldGuard regions: " + e.getMessage());
                }
            }
        }
        return islandWorld;
    }
}
