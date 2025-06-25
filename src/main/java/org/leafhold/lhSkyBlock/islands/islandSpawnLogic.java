package org.leafhold.lhSkyBlock.islands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.leafhold.lhSkyBlock.lhSkyBlock;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class IslandSpawnLogic {
    private FileConfiguration config;
    private final Integer y;
    private final Integer spacing;

    public IslandSpawnLogic(lhSkyBlock plugin) {
        this.config = plugin.getConfig();
        this.y = config.getInt("island.y-coordinate");
        this.spacing = config.getInt("island.spacing");
    }

    public Location IslandLocation(Integer islandIndex, World world) {

        if (islandIndex == 0) {
            return new Location(world, 0, y, 0);
        }

        Integer layer = 1;
        Integer totalPrevious = 0;

        while (true) {
            Integer islandInLayer = 8 * layer;
            if (islandIndex <= totalPrevious + islandInLayer)
                break;
            totalPrevious += islandInLayer;
            layer++;
        }
        
        Integer posInLayer = islandIndex - totalPrevious - 1;

        Integer x = 0, z = 0;

        if (posInLayer < 2 * layer) {
            x = layer;
            z = -layer + posInLayer;
        } else if (posInLayer < 4 * layer) {
            x = layer - (posInLayer - 2 * layer);
            z = layer;
        } else if (posInLayer < 6 * layer) {
            x = -layer;
            z = layer - (posInLayer - 4 * layer);
        } else {
            x = -layer + (posInLayer - 6 * layer);
            z = -layer;
        }

        return new Location(world, x * spacing, y, z * spacing);
    }

    public boolean pasteSchematic(String schematicName, Location location) {
        File schematicFile = new File(lhSkyBlock.getInstance().getDataFolder(), "schematics/" + schematicName);
        if (!schematicFile.exists()) {
            lhSkyBlock.getInstance().getLogger().severe("Schematic file " + schematicName + " does not exist!");
            return false;
        }
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            lhSkyBlock.getInstance().getLogger().severe("Unsupported schematic format for file: " + schematicName);
            return false;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            lhSkyBlock.getInstance().getLogger().severe("Failed to read schematic file: " + e.getMessage());
            return false;
        }
        if (clipboard == null) {
            lhSkyBlock.getInstance().getLogger().severe("Failed to load clipboard from schematic file: " + schematicName);
            return false;
        }
        try {
            com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
            com.sk89q.worldedit.function.operation.Operation operation = new com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(com.sk89q.worldedit.math.BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .build();
            com.sk89q.worldedit.function.operation.Operations.complete(operation);
            editSession.close();
            return true;
        } catch (Exception e) {
            lhSkyBlock.getInstance().getLogger().severe("Failed to paste schematic: " + e.getMessage());
            return false;
        }
    }
}