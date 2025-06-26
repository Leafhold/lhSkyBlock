package org.leafhold.lhSkyBlock.islands;

import org.bukkit.configuration.file.FileConfiguration;
import org.leafhold.lhSkyBlock.lhSkyBlock;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IslandSpawning {
    private static lhSkyBlock plugin;
    private static FileConfiguration config;
    private static Integer islandSpawnY;
    private static Integer islandSpacing;
    
    public IslandSpawning(lhSkyBlock plugin) {
        this.plugin = plugin;
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
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())).build();
            Operations.complete(operation);
            editSession.close();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste island schematic: " + e.getMessage());
            return false;
        }
    }
}
