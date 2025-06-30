package org.leafhold.lhSkyBlock.commands;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.islands.IslandSpawning;
import org.leafhold.lhSkyBlock.utils.DatabaseManager;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class VisitCommand implements CommandExecutor {
    private lhSkyBlock plugin;
    private DatabaseManager databaseManager;

    public VisitCommand(lhSkyBlock plugin) {
        this.plugin = plugin;
        this.databaseManager = DatabaseManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /visit <player>").color(NamedTextColor.RED));
            return true;
        }
        String targetPlayerName = args[0];
        OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            player.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return true;
        }
        List<Object> islandsList = null;
        try {
            islandsList = databaseManager.getIslandsByOwner(targetPlayer.getUniqueId().toString());
        } catch (Exception e) {
            player.sendMessage(Component.text("An error occurred while retrieving the island data.").color(NamedTextColor.RED));
            plugin.getLogger().severe("Error retrieving islands for player " + targetPlayerName + ": " + e.getMessage());
            return true;
        }
        if (islandsList == null || islandsList.isEmpty()) {
            player.sendMessage(Component.text("This player has no islands.").color(NamedTextColor.RED));
            return true;
        }
        if (islandsList.size() == 1) {
            Object islandData = islandsList.get(0);
            visitIsland(player, islandData);
            return true;
        } else {
            //todo Implement an island selection menu
            return true;
        }
    }

    private void visitIsland(Player player, Object islandData) {
        if (islandData == null) {
            player.sendMessage(Component.text("Island data is not available.").color(NamedTextColor.RED));
            return;
        }
        List<Object> islandObject = (List<Object>) islandData;
        Boolean isPublic = (Boolean) islandObject.get(3);
        if (!isPublic) {
            player.sendMessage(Component.text("This island is private. You cannot visit it.").color(NamedTextColor.RED));
            return;
        }
        Integer islandId = (Integer) islandObject.get(0);
        World islandWorld = Bukkit.getWorld("islands");
        Location islandLocation = IslandSpawning.getIslandSpawnLocation(islandId, islandWorld);
        if (islandLocation == null) {
            player.sendMessage(Component.text("Island location not found.").color(NamedTextColor.RED));
            return;
        }
        player.teleportAsync(islandLocation, TeleportCause.COMMAND);

    }
}
