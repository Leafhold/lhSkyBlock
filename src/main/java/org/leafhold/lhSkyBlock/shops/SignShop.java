package org.leafhold.lhSkyBlock.shops;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Listener;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerInteractEvent;

import net.citizensnpcs.api.persistence.Persist;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class SignShop implements Listener {
    private static lhSkyBlock plugin;
    private static FileConfiguration config;

    public SignShop(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    private boolean isSignShop(Block block) {
        if (block == null || !(block.getState() instanceof Sign)) return false;
        PersistentDataContainer data = ((Sign) block.getState()).getPersistentDataContainer();
        if (!data.has(new NamespacedKey(plugin, "shop_type"), PersistentDataType.STRING)) return false;
        return true;
    }

    private boolean isShopItemSetup(Sign sign) {
        PersistentDataContainer data = sign.getPersistentDataContainer();
        NamespacedKey itemKey = new NamespacedKey(plugin, "item");
        if (!data.has(itemKey, PersistentDataType.BYTE_ARRAY)) {
            return false;
        }
        byte[] itemBytes = data.get(itemKey, PersistentDataType.BYTE_ARRAY);
        if (itemBytes == null) {
            return false;
        }
        ItemStack item;
        try {
            item = ItemStack.deserializeBytes(itemBytes);
        } catch (Exception e) {
            return false;
        }
        return item != null;
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Sign sign = (Sign) event.getBlock().getState();

        BlockFace attachedFace = BlockFace.DOWN;
        if (event.getBlock().getBlockData() instanceof Directional) {
            attachedFace = ((Directional) event.getBlock().getBlockData()).getFacing().getOppositeFace();
        }
        Block relativeBlock = event.getBlock().getRelative(attachedFace);

        if (attachedFace == BlockFace.DOWN || attachedFace == BlockFace.UP) return;
        if (relativeBlock == null || !relativeBlock.getType().toString().toLowerCase().contains("chest")) return;

        String[] lines = event.getLines();

        if (!lines[0].equalsIgnoreCase("[sell]") && !lines[0].equalsIgnoreCase("[buy]")) return;
        Boolean sell = "[sell]".equalsIgnoreCase(lines[0]);

        if (lines[1].isEmpty()) {
            player.sendMessage(Component.text("You must specify the item quantity").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        if (lines[2].isEmpty()) {
            player.sendMessage(Component.text("You must specify the item price").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        Integer amount;
        Double price;

        try {
            amount = Integer.parseInt(lines[1]);
            price = Double.parseDouble(lines[2]);
            if (amount <= 0 || price <= 0) {
                player.sendMessage(Component.text("Amount and price must be greater than zero").color(NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount or price format").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }
        event.setLine(0, sell ? "Sell" : "Buy");
        event.setLine(1, String.valueOf(amount));
        event.setLine(2, String.format("$%.2f", price));
        event.setLine(3, player.getName());
        PersistentDataContainer data = sign.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, "shop_type"), PersistentDataType.STRING, sell ? "sell" : "buy");
        data.set(new NamespacedKey(plugin, "item_amount"), PersistentDataType.INTEGER, amount);
        data.set(new NamespacedKey(plugin, "item_price"), PersistentDataType.DOUBLE, price);
        data.set(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING, player.getUniqueId().toString());
        sign.update();
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isSignShop(event.getClickedBlock()) || !event.getAction().isRightClick()) return;
        event.setCancelled(true);
        Sign sign = (Sign) event.getClickedBlock().getState();
        PersistentDataContainer data = sign.getPersistentDataContainer();

        if (isShopItemSetup(sign)) {
            String shopType = data.get(new NamespacedKey(plugin, "shop_type"), PersistentDataType.STRING);
            int amount = data.get(new NamespacedKey(plugin, "item_amount"), PersistentDataType.INTEGER);
            double price = data.get(new NamespacedKey(plugin, "item_price"), PersistentDataType.DOUBLE);
            String ownerUUID = data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING);

            if (ownerUUID.equals(player.getUniqueId().toString())) {
                if (shopType.equals("sell")) player.sendMessage(Component.text("You cannot buy from your own shop").color(NamedTextColor.RED));
                else player.sendMessage(Component.text("You cannot sell to your own shop").color(NamedTextColor.RED));
            }
        } else {
            UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
            if (ownerUUID.equals(player.getUniqueId())) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(Component.text("You must hold a valid item to set up the shop").color(NamedTextColor.RED));
                    return;
                }
                data.set(new NamespacedKey(plugin, "item"), PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
                sign.update();
                player.sendMessage(Component.text("Shop item set to " + item.getType().name()).color(NamedTextColor.GREEN));
            }
        }
    }
}
