package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    private final lhSkyBlock plugin;
    private static FileConfiguration config;

    public PlayerJoinListener(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Location location = player.getLocation();

        if (world == null) {
            plugin.getLogger().warning("Player's world is null, cannot teleport to spawn.");
            return;
        }

        if (world.getName().equalsIgnoreCase(config.getString("main_world"))) {
            Location loc = world.getSpawnLocation();
            loc.add(0.5, 0, 0.5);
            loc.setPitch(0);
            loc.setYaw(180);
            player.teleportAsync(loc, TeleportCause.PLUGIN);
            return;
        }
    }
}
