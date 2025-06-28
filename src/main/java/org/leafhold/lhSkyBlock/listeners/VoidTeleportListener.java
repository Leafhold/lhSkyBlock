package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.islands.IslandSpawning;

import net.kyori.adventure.text.Component;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;

public class VoidTeleportListener implements Listener {
    private final lhSkyBlock plugin;
    private static FileConfiguration config;

    public VoidTeleportListener(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getCause() == DamageCause.VOID) {
                event.setCancelled(true);
                World world = player.getWorld();
                String worldName = world.getName();
                if (worldName.equalsIgnoreCase(config.getString("main-world"))) {
                    Location loc = world.getSpawnLocation();
                    loc.add(0.5, 0, 0.5);
                    loc.setPitch(0);
                    loc.setYaw(180);
                    player.teleportAsync(loc, TeleportCause.PLUGIN);
                    return;
                } 
                if (worldName.equalsIgnoreCase("islands")) {
                    Location location = player.getLocation();
                    Location islandSpawnLocation = IslandSpawning.getIslandIndexFromLocation(location);
                    islandSpawnLocation.setPitch(0);
                    islandSpawnLocation.setYaw(180);
                    islandSpawnLocation.add(0.5, 1, -0.5);
                    player.setInvulnerable(true);
                    var playerTeleport = player.teleportAsync(islandSpawnLocation, TeleportCause.PLUGIN).thenRun(() -> {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.setFallDistance(0);
                            player.setVelocity(new Vector(0, 0, 0));
                            player.setInvulnerable(false);
                        }, 20);
                    });
                    return;
                }
            }
        }
    }
}
