package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.islands.IslandSpawning;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.Bukkit;

public class VoidTeleportListener implements Listener {
    private FileConfiguration config;
    private DatabaseManager databaseManager;

    public VoidTeleportListener(lhSkyBlock plugin) {
        config = plugin.getConfig();
        databaseManager = DatabaseManager.getInstance();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == DamageCause.VOID) {
                Player player = (Player) event.getEntity();
                World world = player.getWorld();
                String worldName = world.getName();
                if (worldName.equalsIgnoreCase(config.getString("main-world"))) {

                    Location loc = world.getSpawnLocation();
                    Double x = loc.getX();
                    Double z = loc.getZ();
                    loc.add(x > 0 ? 0.5 : -0.5, 0.0, z > 0 ? 0.5 : -0.5);
                    loc.setPitch(0);
                    loc.setYaw(180);
                    player.teleportAsync(loc, TeleportCause.PLUGIN);
                    return;
                } 
                if (worldName.equalsIgnoreCase("islands")) {
                    Location location = player.getLocation();
                    Location spawnLocation;
                    Integer islandIndex = IslandSpawning.getIslandIndexFromLocation(location);
                    if (databaseManager.islandExistsByIndex(islandIndex)) {
                        spawnLocation = IslandSpawning.getIslandSpawnLocation(islandIndex, world);
                    } else {
                        World mainWorld = Bukkit.getWorld(config.getString("main-world"));
                        spawnLocation = mainWorld.getSpawnLocation();
                    }
                    Double x = spawnLocation.getX();
                    Double z = spawnLocation.getZ();
                    spawnLocation.add(x > 0 ? 0.5 : -0.5, 0.0, z > 0 ? 0.5 : -0.5);
                    spawnLocation.setPitch(0);
                    spawnLocation.setYaw(180);
                    player.teleportAsync(spawnLocation, TeleportCause.PLUGIN);
                    return;
                }
            }
        }
    }
}
