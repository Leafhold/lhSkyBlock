package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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

                if (world != null && world.getName().equals(config.getString("main_world"))) {
                    Location loc = world.getSpawnLocation();
                    loc.add(0.5, 1, 0.5);
                    loc.setPitch(0);
                    loc.setYaw(180);
                    player.teleportAsync(loc);
                }
            }
        }
    }
}
