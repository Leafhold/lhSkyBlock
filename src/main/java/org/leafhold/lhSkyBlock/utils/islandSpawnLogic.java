package org.leafhold.lhSkyBlock.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class islandSpawnLogic {

    public Location islandLocation(int islandIndex, World world, int y, int spacing) {

        if (islandIndex == 0) {
            return new Location(world, 0, y, 0);
        }

        int layer = 1;
        int totalPrevious = 0;

        while (true) {
            int islandInLayer = 8 * layer;
            if (islandIndex <= totalPrevious + islandInLayer)
                break;
            totalPrevious += islandInLayer;
            layer++;
        }
        
        int posInLayer = islandIndex - totalPrevious - 1;

        int x = 0, z = 0;

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
}
