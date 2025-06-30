package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class PlayerJoinListener implements Listener {
    private static FileConfiguration config;
    private static boolean bJoinMessage;

    public PlayerJoinListener(lhSkyBlock plugin) {
        config = plugin.getConfig();
        bJoinMessage = config.getBoolean("join_message", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        if (bJoinMessage) {
            event.joinMessage(null);
        } else {
            event.joinMessage(Component.text("[")
                .append(Component.text("+").color(NamedTextColor.GREEN))
                .append(Component.text("] " + player.getName())));
        }
        World mainWorld = Bukkit.getWorld(config.getString("main-world"));
        if (world == null || world == mainWorld) {
            Location loc = mainWorld.getSpawnLocation();
            Double x = loc.getX();
            Double z = loc.getZ();
            loc.add(x >= 0 ? 0.5 : -0.5, 0.0, z >= 0 ? 0.5 : -0.5);
            loc.setPitch(0);
            loc.setYaw(180);
            player.teleportAsync(loc, TeleportCause.PLUGIN);
            return;
        }
    }
}
