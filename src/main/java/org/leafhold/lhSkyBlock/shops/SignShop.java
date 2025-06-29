package org.leafhold.lhSkyBlock.shops;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Listener;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.ItemHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;

import net.milkbowl.vault.economy.Economy;

import java.util.UUID;

public class SignShop implements Listener {
    private lhSkyBlock plugin;
    private Economy economy;

    public SignShop(lhSkyBlock plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                plugin.getLogger().info("Vault economy provider found: " + economy.getName());
            } else {
                plugin.getLogger().warning("Vault economy provider not found. Shop transactions will not work.");
            }
        }
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

        String[] lines = event.lines().stream()
            .map(component -> component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component))
            .toArray(String[]::new);

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
        event.line(0, Component.text(sell ? "Selling" : "Buying").color(NamedTextColor.WHITE));
        event.line(1, Component.text(amount).color(NamedTextColor.WHITE));
        event.line(2, Component.text(String.format("$%.2f", price)).color(NamedTextColor.DARK_GREEN));
        event.line(3, Component.text(player.getName()).color(NamedTextColor.WHITE));

        PersistentDataContainer data = sign.getPersistentDataContainer();
        data.set(new NamespacedKey(plugin, "shop_uuid"), PersistentDataType.STRING, UUID.randomUUID().toString());
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
            ItemStack item = ItemStack.deserializeBytes(data.get(new NamespacedKey(plugin, "item"), PersistentDataType.BYTE_ARRAY));
            int amount = data.get(new NamespacedKey(plugin, "item_amount"), PersistentDataType.INTEGER);
            double price = data.get(new NamespacedKey(plugin, "item_price"), PersistentDataType.DOUBLE);
            String ownerUUID = data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

            if (ownerUUID.equals(player.getUniqueId().toString())) {
                if (shopType.equals("sell")) player.sendMessage(Component.text("You cannot buy from your own shop").color(NamedTextColor.RED));
                else player.sendMessage(Component.text("You cannot sell to your own shop").color(NamedTextColor.RED));
                return;
            }
            BlockFace attachedFace = ((Directional) sign.getBlock().getBlockData()).getFacing().getOppositeFace();
            Block chestBlock = sign.getBlock().getRelative(attachedFace);

            Inventory chestInventory = ((InventoryHolder) chestBlock.getState()).getInventory();
            boolean itemFound = false;
            if (chestInventory.containsAtLeast(item, amount)) {
                itemFound = true;
            }

            if (!itemFound) {
                player.sendMessage(Component.text("The shop does not have enough items to complete the transaction").color(NamedTextColor.RED));
                return;
            }

            if (shopType.equals("sell")) {
                if (economy.getBalance(player) < price) {
                    player.sendMessage(Component.text("You do not have enough money to buy this item").color(NamedTextColor.RED));
                    return;
                }
                if (player.getInventory().containsAtLeast(item, amount)) {
                    if (chestInventory.firstEmpty() == -1) { //todo Fix this check to allow for stacking in the last slot
                        player.sendMessage(Component.text("The shop's chest is full").color(NamedTextColor.RED));
                        return;
                    }
                    ItemStack toRemove = item.clone();
                    toRemove.setAmount(amount);
                    player.getInventory().removeItem(toRemove);
                    chestInventory.addItem(toRemove);
                    economy.withdrawPlayer(player, amount);
                    economy.depositPlayer(owner, price);
                    player.sendMessage(Component.text("Sign shop >").color(NamedTextColor.GREEN)
                        .append(Component.text(" Bought " + amount + " " + item.getType().name() + " for $" + price)));
                }
                else {
                    player.sendMessage(Component.text("You do not have enough items to sell").color(NamedTextColor.RED));
                    return;
                }
            } else {
                if (economy.getBalance(owner) < price) {
                    player.sendMessage(Component.text("The shop owner does not have enough money to sell this item").color(NamedTextColor.RED));
                    return;
                }
                if (chestInventory.containsAtLeast(item, amount)) {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage(Component.text("Your inventory is full").color(NamedTextColor.RED));
                        return;
                    }
                    economy.withdrawPlayer(player, amount);
                    economy.depositPlayer(owner, price);
                    ItemStack toAdd = item.clone();
                    toAdd.setAmount(amount);
                    chestInventory.removeItem(toAdd);
                    player.getInventory().addItem(toAdd);
                } else {
                    player.sendMessage(Component.text("The shop does not have enough items to complete the transaction").color(NamedTextColor.RED));
                    return;
                }
            }

            
        } else {
            UUID ownerUUID = UUID.fromString(data.get(new NamespacedKey(plugin, "owner"), PersistentDataType.STRING));
            if (ownerUUID.equals(player.getUniqueId())) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(Component.text("You must hold a valid item to set up the shop").color(NamedTextColor.RED));
                    return;
                }
                BlockFace attachedFace = ((Directional) sign.getBlock().getBlockData()).getFacing().getOppositeFace();
                Block chestBlock = sign.getBlock().getRelative(attachedFace);
                Location chestLocation = chestBlock.getLocation();
                Location location = chestBlock.getLocation().add(0, 1.25, 0);
                if (chestLocation.getX() > 0) location.add(-0.5, 0, 0);
                else location.add(0.5, 0, 0);
                if (chestLocation.getZ() > 0) location.add(0, 0, -0.5);
                else location.add(0, 0, 0.5);

                
                String shopUUID = data.get(new NamespacedKey(plugin, "shop_uuid"), PersistentDataType.STRING);
                ItemHologramData hologramData = new ItemHologramData(shopUUID, location);
                hologramData.setItemStack(item);
                hologramData.setScale(new org.joml.Vector3f(0.5f, 0.5f, 0.5f));
                hologramData.setBillboard(Billboard.FIXED);
                
                FancyHologramsPlugin fancyHologramsPlugin = (FancyHologramsPlugin) Bukkit.getPluginManager().getPlugin("FancyHolograms");
                HologramManager manager = fancyHologramsPlugin.getHologramManager();
                Hologram hologram = manager.create(hologramData);
                manager.addHologram(hologram);

                data.set(new NamespacedKey(plugin, "item"), PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
                sign.update();
                player.sendMessage(Component.text("Shop item set to " + item.getType().name()).color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("This shop is not set up yet.").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isSignShop(block)) {
            Sign sign = (Sign) block.getState();
            PersistentDataContainer data = sign.getPersistentDataContainer();
            String shopUUID = data.get(new NamespacedKey(plugin, "shop_uuid"), PersistentDataType.STRING);
            
            FancyHologramsPlugin fancyHologramsPlugin = (FancyHologramsPlugin) Bukkit.getPluginManager().getPlugin("FancyHolograms");
            HologramManager manager = fancyHologramsPlugin.getHologramManager();
            Hologram hologram = manager.getHologram(shopUUID).orElse(null);
            if (hologram != null) {
                manager.removeHologram(hologram);
            }
            return;
        }
        for (BlockFace face : BlockFace.values()) {
            Block relativeBlock = block.getRelative(face);
            if (isSignShop(relativeBlock)) {
                Sign sign = (Sign) relativeBlock.getState();
                if (sign.getBlockData() instanceof Directional directional) {
                    BlockFace attachedFace = directional.getFacing().getOppositeFace();
                    Block attachedBlock = relativeBlock.getRelative(attachedFace);
                    if (!attachedBlock.equals(block)) continue;
                } else {
                    continue;
                }
                PersistentDataContainer data = sign.getPersistentDataContainer();
                String shopUUID = data.get(new NamespacedKey(plugin, "shop_uuid"), PersistentDataType.STRING);
                
                FancyHologramsPlugin fancyHologramsPlugin = (FancyHologramsPlugin) Bukkit.getPluginManager().getPlugin("FancyHolograms");
                HologramManager manager = fancyHologramsPlugin.getHologramManager();
                Hologram hologram = manager.getHologram(shopUUID).orElse(null);
                if (hologram != null) {
                    manager.removeHologram(hologram);
                }
            }
        }
    }
}
