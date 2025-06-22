package org.leafhold.lhSkyBlock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import org.leafhold.lhSkyBlock.utils.DatabaseManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ShopCommand implements CommandExecutor, Listener {
  private DatabaseManager databaseManager;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
            final TextComponent message = Component.text("Only players can use this command.")
                .color(NamedTextColor.RED);
            sender.sendMessage(message);
            return true;
        }
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance();
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        
  }
  
}
