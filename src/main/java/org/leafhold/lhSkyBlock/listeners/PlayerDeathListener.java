package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class PlayerDeathListener implements Listener {
    private static FileConfiguration config;
    private static boolean keepInventory;

    public PlayerDeathListener(lhSkyBlock plugin) {
        config = plugin.getConfig();
        keepInventory = config.getBoolean("keep-inventory", false);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!(player.getKiller() instanceof Player)) {
            if (keepInventory) event.setKeepInventory(true);
            else event.setKeepInventory(false);
        }
    }
}
