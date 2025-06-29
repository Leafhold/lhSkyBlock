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

public class PlayerLeaveListener implements Listener {
    private final lhSkyBlock plugin;
    private static FileConfiguration config;
    private static boolean bJoinMessage;

    public PlayerLeaveListener(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        bJoinMessage = config.getBoolean("join_message", true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (bJoinMessage) {
            event.joinMessage(null);
        } else {
            event.joinMessage(Component.text("[")
                .append(Component.text("+").color(NamedTextColor.GREEN))
                .append(Component.text("] " + player.getName())));
        }
    }
}
