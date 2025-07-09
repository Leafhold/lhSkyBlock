package org.leafhold.lhSkyBlock.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.command.TabCompleter;

import org.leafhold.lhSkyBlock.lhSkyBlock;
import org.leafhold.lhSkyBlock.shops.ShopHolder;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.npc.NPC;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShopCommand implements CommandExecutor, Listener, TabCompleter {
    private lhSkyBlock plugin;
    private FileConfiguration config;
    private NPCRegistry npcRegistry;
    private Economy economy;

    public ShopCommand(lhSkyBlock plugin) {
        this.plugin = plugin;

        File configFile = new File(plugin.getDataFolder(), "shops.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        config = new YamlConfiguration();
        try {
            this.config.load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        npcRegistry = CitizensAPI.getNPCRegistry();
        setupEconomy();
    }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Please specify an NPC ID.").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("Please specify a shop name.").color(NamedTextColor.RED));
                    return true;
                }
                try {
                    Integer npcId = Integer.parseInt(args[1]);
                    String shopName = args[2];
                    return createShop(player, npcId, shopName);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid NPC ID. Please provide a valid number.").color(NamedTextColor.RED));
                    return true;
                }
            case "reload":
                reloadConfig();
                player.sendMessage(Component.text("Shop configuration reloaded.").color(NamedTextColor.GREEN));
                return true;
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Please specify a shop name to delete.").color(NamedTextColor.RED));
                    return true;
                }
                String shopName = args[1];
                if (config.contains("shops." + shopName)) {
                    config.set("shops." + shopName, null);
                    try {
                        config.save(new File(plugin.getDataFolder(), "shops.yml"));
                        player.sendMessage(Component.text("Shop '" + shopName + "' deleted successfully.").color(NamedTextColor.GREEN));
                    } catch (Exception e) {
                        player.sendMessage(Component.text("Failed to delete shop configuration.").color(NamedTextColor.RED));
                        e.printStackTrace();
                    }
                } else {
                    player.sendMessage(Component.text("This shop does not exist.").color(NamedTextColor.RED));
                }
                return true;
        }

        return false;
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

    private boolean createShop(Player player, Integer npcId, String shopName) {
        if (config.contains("shops." + shopName)) {
            player.sendMessage(Component.text("A shop with this name already exists.").color(NamedTextColor.RED));
            return true;
        }
        for (String shop : config.getConfigurationSection("shops").getKeys(false)) {
            if (config.getString("shops." + shop + ".npc", "").equalsIgnoreCase(npcId.toString())) {
                player.sendMessage(Component.text("A shop with this npc already exists.").color(NamedTextColor.RED));
                return true;
            }
        }

        NPC shopNPC = npcRegistry.getById(npcId);
        if (shopNPC == null) {
            player.sendMessage(Component.text("NPC with ID " + npcId + " does not exist.").color(NamedTextColor.RED));
            return true;
        }

        config.set("shops." + shopName + ".npc", shopNPC.getId());
        try {
            config.save(new File(plugin.getDataFolder(), "shops.yml"));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to save shop configuration.").color(NamedTextColor.RED));
            e.printStackTrace();
            return true;
        }

        player.sendMessage(Component.text("Shop '" + shopName + "' created successfully.").color(NamedTextColor.GREEN));
        return true;
    }

    private void openShop(Player player, String shopName) {
        if (!config.contains("shops." + shopName)) {
            player.sendMessage(Component.text("This shop does not exist.").color(NamedTextColor.RED));
            return;
        }
        
        String name = config.getString("shops." + shopName + ".name", shopName);
        if (name == null || name.isEmpty()) name = shopName;
        
        Inventory shopInventory = Bukkit.createInventory(new ShopHolder(), 54, Component.text(name));
        if (config.getConfigurationSection("shops." + shopName + ".items") != null) {
            for (String itemKey : config.getConfigurationSection("shops." + shopName + ".items").getKeys(false)) {
                ItemStack item = new ItemStack(Material.getMaterial(itemKey.toUpperCase()));
                Integer slot = config.getInt("shops." + shopName + ".items." + itemKey + ".slot", -1);
                Integer defaultAmount = config.getInt("shops." + shopName + ".items." + itemKey + ".default_amount", 1);
                Double sellPrice = config.getDouble("shops." + shopName + ".items." + itemKey + ".sell.price");
                Double buyPrice = config.getDouble("shops." + shopName + ".items." + itemKey + ".buy.price");
                
                if (item != null && slot >= 0 && slot < shopInventory.getSize()) {
                    item.setAmount(defaultAmount);
                    ItemMeta meta = item.getItemMeta();
                    meta.lore(java.util.Arrays.asList(
                        Component.text("Buy: $").append(Component.text(buyPrice * defaultAmount)).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                        Component.text("Sell: $").append(Component.text(sellPrice * defaultAmount)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.text("Left click to buy").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false),
                        Component.text("Right click to sell").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false)
                        ));
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "shop_item");
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE, buyPrice);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE, sellPrice);
                    item.setItemMeta(meta);
                    shopInventory.setItem(slot, item);
                }
            }
            player.openInventory(shopInventory);
        } else {
            player.sendMessage(Component.text("This shop is empty. No items have been configured yet.").color(NamedTextColor.YELLOW));
            return;
        }
    }

    private void openTransactionMenu(Player player, Component shopName, ItemStack item) {
        String shopKey = null;
        Double price;
        boolean isBuying = item.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "item_buying"), PersistentDataType.BOOLEAN);
        price = isBuying ? 
            item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE) :
            item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE);
        Inventory transactionMenu = Bukkit.createInventory(new ShopHolder(), 54, Component.text(isBuying ? "Buy" : "Sell"));
        if (config.getConfigurationSection("shops") != null) {
            for (String shop : config.getConfigurationSection("shops").getKeys(false)) {
                String shopNameKey = config.getString("shops." + shop + ".name", shop);
                if (shopName != null && PlainTextComponentSerializer.plainText().serialize(shopName).equalsIgnoreCase(shopNameKey)) {
                    shopKey = shop;
                    break;
                }
            }
        }
        if (shopKey == null) {
            player.sendMessage(Component.text("Could not find the shop named " + shopName + ".").color(NamedTextColor.RED));
            return;
        }
        if (config.contains("shops." + shopKey + ".items." + item.getType().name().toLowerCase())) {
            ItemMeta itemMeta = item.getItemMeta();
            if (isBuying) {
                itemMeta.lore(java.util.Collections.singletonList(
                    Component.text("Price: $" + price * item.getAmount()).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                ));
            } else {
                itemMeta.lore(java.util.Collections.singletonList(
                    Component.text("Price: $" + price * item.getAmount()).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                ));
            }
            itemMeta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "item_role"));
            itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_key"), PersistentDataType.STRING, shopKey);
            item.setItemMeta(itemMeta);
            transactionMenu.setItem(22, item);

            ItemStack increaseItem = new ItemStack(Material.LIME_DYE);
            ItemMeta increaseMeta = increaseItem.getItemMeta();
            increaseMeta.displayName(Component.text("Add 1").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            increaseMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            increaseMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "increase_item");
            increaseItem.setItemMeta(increaseMeta);
            transactionMenu.setItem(24, increaseItem);

            ItemStack increase8Item = new ItemStack(Material.LIME_DYE);
            ItemMeta increase8Meta = increase8Item.getItemMeta();
            increase8Meta.displayName(Component.text("Add 8").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            increase8Meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            increase8Meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "increase_item_8");
            increase8Item.setItemMeta(increase8Meta);
            increase8Item.setAmount(8);
            transactionMenu.setItem(25, increase8Item);

            ItemStack increaseAllItem = new ItemStack(Material.LIME_DYE);
            ItemMeta increaseAllMeta = increaseAllItem.getItemMeta();
            increaseAllMeta.displayName(Component.text("Set to max").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            increaseAllMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            increaseAllMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "increase_item_all");
            increaseAllItem.setItemMeta(increaseAllMeta);
            increaseAllItem.setAmount(item.getMaxStackSize());
            transactionMenu.setItem(26, increaseAllItem);

            ItemStack decreaseItem = new ItemStack(Material.RED_DYE);
            ItemMeta decreaseMeta = decreaseItem.getItemMeta();
            decreaseMeta.displayName(Component.text("Remove 1").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            decreaseMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            decreaseMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "decrease_item");
            decreaseItem.setItemMeta(decreaseMeta);
            transactionMenu.setItem(20, decreaseItem);

            ItemStack decrease8Item = new ItemStack(Material.RED_DYE);
            ItemMeta decrease8Meta = decrease8Item.getItemMeta();
            decrease8Meta.displayName(Component.text("Remove 8").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            decrease8Meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            decrease8Meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "decrease_item_8");
            decrease8Item.setItemMeta(decrease8Meta);
            decrease8Item.setAmount(8);
            transactionMenu.setItem(19, decrease8Item);
            
            ItemStack decreaseAllItem = new ItemStack(Material.RED_DYE);
            ItemMeta decreaseAllMeta = decreaseAllItem.getItemMeta();
            decreaseAllMeta.displayName(Component.text("Set to min").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            decreaseAllMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            decreaseAllMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "decrease_item_all");
            decreaseAllItem.setItemMeta(decreaseAllMeta);
            decreaseAllItem.setAmount(64);
            transactionMenu.setItem(18, decreaseAllItem);

            ItemStack confirmItem = new ItemStack(Material.GREEN_STAINED_GLASS);
            ItemMeta confirmMeta = confirmItem.getItemMeta();
            confirmMeta.displayName(Component.text("Confirm Transaction").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            confirmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            confirmMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "confirm_item");
            confirmItem.setItemMeta(confirmMeta);
            transactionMenu.setItem(48, confirmItem);

            ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS);
            ItemMeta cancelMeta = cancelItem.getItemMeta();
            cancelMeta.displayName(Component.text("Cancel Transaction").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            cancelMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING, "transaction_item");
            cancelMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING, "cancel_item");
            cancelItem.setItemMeta(cancelMeta);
            transactionMenu.setItem(50, cancelItem);


            player.openInventory(transactionMenu);
        } else {
            player.sendMessage(Component.text("There was an error retrieving this item from the shop.").color(NamedTextColor.RED));
            return;
        }
    }

    private List<Component> updateItemPrice(ItemStack item) {
        Double price;
        Boolean isBuying = item.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "item_buying"), PersistentDataType.BOOLEAN);
        price = isBuying ? 
            item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE) :
            item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE);
        
        if (isBuying) {
            return java.util.Collections.singletonList(
                Component.text("Price: $" + price * item.getAmount()).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            );
        } else {
            return java.util.Collections.singletonList(
                Component.text("Price: $" + price * item.getAmount()).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
            ); 
        }
    }

    private void reloadConfig() {
        try {
            config.load(new File(plugin.getDataFolder(), "shops.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload shops configuration: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("delete");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            for (NPC npc : npcRegistry) {
                completions.add(String.valueOf(npc.getId()));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            if (config.getConfigurationSection("shops") != null) {
                completions.addAll(config.getConfigurationSection("shops").getKeys(false));
            }
        }

        return completions;
    }

    @EventHandler
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity != null && npcRegistry.isNPC(entity)) {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            if (npc != null) {
                String shopName = null;
                if (config.getConfigurationSection("shops") != null) {
                    for (String shop : config.getConfigurationSection("shops").getKeys(false)) {
                        if (config.getInt("shops." + shop + ".npc") == npc.getId()) {
                            shopName = shop;
                            break;
                        }
                    }
                }
                
                if (shopName != null) {
                    event.setCancelled(true);
                    openShop(player, shopName);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().getHolder() instanceof ShopHolder) {
                
                event.setCancelled(true);
                
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    String itemRole = event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, "item_role"), PersistentDataType.STRING);
                    
                    if (itemRole != null) {
                        ItemStack item = event.getCurrentItem();
                        Double price;
                        if (item.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE)) {
                            price = item.getItemMeta().getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE);
                        } else {
                            price = item.getItemMeta().getPersistentDataContainer()
                                .get(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE);
                        }
                        switch(itemRole) {
                            case "shop_item":
                                Double buyPrice = item.getItemMeta().getPersistentDataContainer()
                                    .get(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE);
                                Double sellPrice = item.getItemMeta().getPersistentDataContainer()
                                    .get(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE);
                                if (event.isLeftClick() && buyPrice != null) {
                                    ItemMeta itemMeta = item.getItemMeta();
                                    itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_buying"), PersistentDataType.BOOLEAN, true);
                                    item.setItemMeta(itemMeta);
                                    openTransactionMenu(player, event.getView().title(), item);
                                } else if (event.isRightClick() && sellPrice != null) {
                                    ItemMeta itemMeta = item.getItemMeta();
                                    itemMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "item_buying"), PersistentDataType.BOOLEAN, false);
                                    item.setItemMeta(itemMeta);
                                    openTransactionMenu(player, event.getView().title(), item);
                                }
                                break;
                            case "transaction_item":
                                ItemStack transactedItem = event.getClickedInventory().getItem(22);
                                ItemMeta meta = transactedItem.getItemMeta();
                                String itemKey = item.getItemMeta().getPersistentDataContainer()
                                    .get(new NamespacedKey(plugin, "item_key"), PersistentDataType.STRING);
                                Boolean isBuying = transactedItem.getItemMeta().getPersistentDataContainer()
                                    .get(new NamespacedKey(plugin, "item_buying"), PersistentDataType.BOOLEAN);
                                price = isBuying ?
                                    transactedItem.getItemMeta().getPersistentDataContainer()
                                        .get(new NamespacedKey(plugin, "item_buy_price"), PersistentDataType.DOUBLE) :
                                    transactedItem.getItemMeta().getPersistentDataContainer()
                                        .get(new NamespacedKey(plugin, "item_sell_price"), PersistentDataType.DOUBLE);

                                if (itemKey != null) {
                                    String shopName =  transactedItem.getItemMeta().getPersistentDataContainer()
                                        .get(new NamespacedKey(plugin, "shop_key"), PersistentDataType.STRING);
                                    Integer minAmount = isBuying ?
                                        config.getInt("shops." + shopName + ".items." + transactedItem.getType().name().toLowerCase() + ".buy.min_amount", 1) :
                                        config.getInt("shops." + shopName + ".items." + transactedItem.getType().name().toLowerCase() + ".sell.min_amount", 1);
                                    switch (itemKey) {
                                        case "cancel_item":
                                            player.closeInventory();
                                            break;
                                        case "confirm_item":
                                            Double money = price * transactedItem.getAmount();
                                            if (item != null && economy != null && price != null) {
                                                if (isBuying) {
                                                    if (economy.getBalance(player) >= money) {
                                                        economy.withdrawPlayer(player, money);
                                                        player.getInventory().addItem(new ItemStack(transactedItem.getType(), transactedItem.getAmount()));
                                                        player.sendMessage(Component.text("Shop > ").color(NamedTextColor.GREEN)
                                                            .append(Component.text("You bought " + transactedItem.getType().name() + " x " + transactedItem.getAmount() + " for $" + money)));
                                                        player.closeInventory();
                                                    } else {
                                                        player.sendMessage(Component.text("Shop > ").color(NamedTextColor.GREEN)
                                                            .append(Component.text("You do not have enough money to buy this item.").color(NamedTextColor.RED)));
                                                    }
                                                } else {
                                                    if (player.getInventory().contains(transactedItem.getType(), transactedItem.getAmount())) {
                                                        economy.depositPlayer(player, money);
                                                        player.getInventory().removeItem(new ItemStack(transactedItem.getType(), transactedItem.getAmount()));
                                                        player.sendMessage(Component.text("Shop > ").color(NamedTextColor.GREEN)
                                                            .append(Component.text("You sold " + transactedItem.getType().name() + " x " + transactedItem.getAmount() + " for $" + money)
                                                                .color(NamedTextColor.GREEN)));
                                                        player.closeInventory();
                                                    } else {
                                                        player.sendMessage(Component.text("Shop > ").color(NamedTextColor.GREEN)
                                                            .append(Component.text("You do not have enough of this item to sell.").color(NamedTextColor.RED)));
                                                    }
                                                }
                                            }
                                            break;
                                        case "increase_item":
                                            if (transactedItem != null) {
                                                int currentAmount = transactedItem.getAmount();
                                                if (currentAmount < transactedItem.getMaxStackSize()) {
                                                    transactedItem.setAmount(currentAmount + 1);
                                                }
                                                meta.lore(updateItemPrice(transactedItem));
                                                transactedItem.setItemMeta(meta);
                                            }
                                            break;
                                        case "increase_item_8":
                                            if (transactedItem != null) {
                                                int currentAmount = transactedItem.getAmount();
                                                if (currentAmount + 8 <= transactedItem.getMaxStackSize()) {
                                                    transactedItem.setAmount(currentAmount + 8);
                                                } else {
                                                    transactedItem.setAmount(transactedItem.getMaxStackSize());
                                                }
                                                meta.lore(updateItemPrice(transactedItem));
                                                transactedItem.setItemMeta(meta);
                                            }
                                            break;
                                        case "increase_item_all":
                                            if (transactedItem != null) {
                                                transactedItem.setAmount(transactedItem.getMaxStackSize());
                                            }
                                            meta.lore(updateItemPrice(transactedItem));
                                            transactedItem.setItemMeta(meta);
                                            break;
                                        case "decrease_item":
                                            if (transactedItem != null) {
                                                int currentAmount = transactedItem.getAmount();
                                                if (currentAmount > minAmount) {
                                                    transactedItem.setAmount(currentAmount - 1);
                                                }
                                                meta.lore(updateItemPrice(transactedItem));
                                                transactedItem.setItemMeta(meta);
                                            }
                                            break;
                                        case "decrease_item_8":
                                            if (transactedItem != null) {
                                                int currentAmount = transactedItem.getAmount();
                                                if (currentAmount - 8 >= minAmount) {
                                                    transactedItem.setAmount(currentAmount - 8);
                                                } else {
                                                    transactedItem.setAmount(minAmount);
                                                }
                                                meta.lore(updateItemPrice(transactedItem));
                                                transactedItem.setItemMeta(meta);
                                            }
                                            break;
                                        case "decrease_item_all":
                                            if (transactedItem != null) {
                                                transactedItem.setAmount(minAmount);
                                            }
                                            meta.lore(updateItemPrice(transactedItem));
                                            transactedItem.setItemMeta(meta);
                                            break;
                                    }
                                }
                                break;
                        }
                    }
                }
            }
            else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }
}
