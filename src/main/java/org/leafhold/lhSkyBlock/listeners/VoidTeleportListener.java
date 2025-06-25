package org.leafhold.lhSkyBlock.listeners;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class VoidTeleportListener implements Listener {
    private final lhSkyBlock plugin;

    public VoidTeleportListener(lhSkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getCause() == DamageCause.VOID) {
                event.setCancelled(true);
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }
}
