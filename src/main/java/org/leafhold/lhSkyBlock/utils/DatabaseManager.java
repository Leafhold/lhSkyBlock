package org.leafhold.lhSkyBlock.utils;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class DatabaseManager {
    private lhSkyBlock plugin;
    private static HikariConfig hikariConfig;
    private static HikariDataSource dataSource;

    public DatabaseManager(lhSkyBlock plugin) {
        this.plugin = plugin;
    }

    public static DatabaseManager getInstance() {
        return new DatabaseManager(lhSkyBlock.getInstance());
    }

    public Connection getConnection() {
        try {
            if (dataSource == null) {
                dataSource = new HikariDataSource(hikariConfig);
            }
            return dataSource.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to the database: " + e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
    }

    public void connect() throws SQLException {
        FileConfiguration config = lhSkyBlock.getInstance().getConfig();
        try {
            hikariConfig = new HikariConfig();
            String host = config.getString("db.host");
            Integer port = config.getInt("db.port");
            String database = config.getString("db.database");
            if (host == null || port == null || database == null) {
                throw new SQLException("Database configuration is incomplete");
            }
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getString("db.user"));
            hikariConfig.setPassword(config.getString("db.password"));
            hikariConfig.setMaximumPoolSize(config.getInt("db.pool-size"));
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(hikariConfig);

            createTable();
        } catch (Exception e) {
            lhSkyBlock.getInstance().getLogger().severe("Failed to configure HikariCP: " + e.getMessage());
            throw new SQLException("HikariCP configuration failed");
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            lhSkyBlock.getInstance().getLogger().info("Disconnected from the database pool successfully!");
        }
    }

    private void createTable() throws SQLException {
        String islandTable = "CREATE TABLE IF NOT EXISTS islands (" +
            "island_index INT NOT NULL PRIMARY KEY," +
            "uuid CHAR(36) NOT NULL UNIQUE," +
            "owner CHAR(36) NOT NULL," +
            "name TEXT NOT NULL," +
            "world TEXT NOT NULL," +
            "is_public BOOLEAN NOT NULL DEFAULT false," +
            "UNIQUE (island_index, world)" +
            ");";
        String memberTable =
            "CREATE TABLE IF NOT EXISTS island_members (" +
            "island_uuid CHAR(36) NOT NULL," +
            "member_uuid CHAR(36) NOT NULL," +
            "role TEXT NOT NULL DEFAULT 'member'," +
            "PRIMARY KEY (island_uuid, member_uuid)," +
            "FOREIGN KEY (island_uuid) REFERENCES islands(uuid)" +
            ");";
        String playerKeysTable = "CREATE TABLE IF NOT EXISTS player_keys (" +
            "player_uuid CHAR(36) NOT NULL," +
            "key_type VARCHAR(32) NOT NULL," +
            "amount INT NOT NULL DEFAULT 0," +
            "PRIMARY KEY (player_uuid, key_type)" +
            ");";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(islandTable);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(memberTable);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(playerKeysTable);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    public Object[] createIsland(UUID ownerUUID, String name, String world) throws SQLException {
        String sql = "SELECT uuid FROM islands WHERE owner = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ownerUUID.toString());
            if (preparedStatement.executeQuery().next()) {
                return null;
            }
            preparedStatement.close();
        }
        UUID islandUUID = java.util.UUID.randomUUID();
        int islandIndex = -1;

        sql = "INSERT INTO islands (uuid, owner, name, world, island_index) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            Integer newIslandIndex = getNewIslandIndex();
            if (newIslandIndex == null) {
                throw new SQLException("Failed to get new island index");
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, islandUUID.toString());
            preparedStatement.setString(2, ownerUUID.toString());
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, world);
            preparedStatement.setInt(5, newIslandIndex);
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                islandIndex = generatedKeys.getInt(1);
            } else {
                String fetchSql = "SELECT island_index FROM islands WHERE uuid = ?";
                PreparedStatement fetchStmt = connection.prepareStatement(fetchSql);
                fetchStmt.setString(1, islandUUID.toString());
                ResultSet rs = fetchStmt.executeQuery();
                if (rs.next()) {
                    islandIndex = rs.getInt("island_index");
                }
                fetchStmt.close();
            }
            preparedStatement.close();
        }
        return new Object[] { islandUUID, islandIndex };
    }

    public List<Object> getIslandsByOwner(String ownerUUID) throws SQLException {
        String sql = "SELECT uuid, owner, name, is_public FROM islands WHERE owner = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ownerUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Object> islands = new ArrayList<>();
            while (resultSet.next()) {
                String islandUUID = resultSet.getString("uuid");
                String owner = resultSet.getString("owner");
                String name = resultSet.getString("name");
                boolean isPublic = resultSet.getBoolean("is_public");
                islands.add(new Object[] { islandUUID, owner, name, isPublic });
            }
            return islands;
        }
    }

    public Object getIslandByUUID(UUID islandUUID) throws SQLException {
        String sql = "SELECT uuid, owner, name, is_public, island_index FROM islands WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                String owner = resultSet.getString("owner");
                String name = resultSet.getString("name");
                boolean isPublic = resultSet.getBoolean("is_public");
                Integer islandIndex = resultSet.getInt("island_index");
                return new Object[] { uuid, owner, name, isPublic, islandIndex };
            }
        }
        return null;
                
    }

    public boolean visitorsAllowed(String islandUUID) throws SQLException {
        String sql = "SELECT is_public FROM islands WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getBoolean("is_public");
            }
            return false;
        }
    }

    public void toggleVisitors(UUID islandUUID) throws SQLException {
        String sql = "UPDATE islands SET is_public = NOT is_public WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID.toString());
            preparedStatement.executeUpdate();
        }
    }

    public Boolean deleteIsland(Player player, String islandUUID) throws SQLException {
        String sql = "DELETE FROM islands WHERE uuid = ? AND owner = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID);
            preparedStatement.setString(2, player.getUniqueId().toString());
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                return false;
            } else {
                return true;
            }
        }
    }

    private Integer getNewIslandIndex() {
        String sql = "SELECT island_index FROM islands ORDER BY island_index ASC";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            int expected = 0;
            while (resultSet.next()) {
                int current = resultSet.getInt("island_index");
                if (current != expected) {
                    return expected;
                }
                expected++;
            }
            return expected;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get new island index: " + e.getMessage());
            return null;
        }
    }

    public boolean islandExistsByUUID(String islandUUID) {
        String sql = "SELECT COUNT(*) FROM islands WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if island exists: " + e.getMessage());
        }
        return false;
    }

    public boolean islandExistsByIndex(int islandIndex) {
        String sql = "SELECT COUNT(*) FROM islands WHERE island_index = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, islandIndex);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if island exists: " + e.getMessage());
        }
        return false;
    }

    public List<Object> getKeys(UUID playerUUID) {
        String sql = "SELECT key_type, amount FROM player_keys WHERE player_uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            List<Object> keys = new ArrayList<>();
            while (resultSet.next()) {
                String keyType = resultSet.getString("key_type");
                int amount = resultSet.getInt("amount");
                keys.add(new Object[] { keyType, amount });
            }
            return keys;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get keys for player " + playerUUID + ": " + e.getMessage());
            return null;
        }
    }

    public List<ItemStack> getKeysForPlayer(UUID playerUUID) {
        String sql = "SELECT key_type, amount FROM player_keys WHERE player_uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            List<ItemStack> keys = new ArrayList<>();
            while (resultSet.next()) {
                String keyType = resultSet.getString("key_type");
                Integer amount = resultSet.getInt("amount");
                if (amount <= 0) continue;
                switch (keyType.toLowerCase()) {
                    case "vote":
                        ItemStack voteKey = new ItemStack(Material.TRIAL_KEY);
                        voteKey.setAmount(amount);
                        ItemMeta voteMeta = voteKey.getItemMeta();
                        voteMeta.itemName(Component.text("Vote key").color(NamedTextColor.GREEN));
                        voteMeta.lore(List.of(Component.text("Use this key to open vote crates").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                        voteMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING, "vote");
                        voteKey.setItemMeta(voteMeta);
                        keys.add(voteKey);
                        break;
                    case "bronze":
                        ItemStack bronzeKey = new ItemStack(Material.TRIAL_KEY);
                        bronzeKey.setAmount(amount);
                        ItemMeta bronzeMeta = bronzeKey.getItemMeta();
                        bronzeMeta.itemName(Component.text("Bronze key").color(NamedTextColor.GOLD));
                        bronzeMeta.lore(List.of(Component.text("Use this key to open bronze crates").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                        bronzeMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING, "bronze");
                        bronzeKey.setItemMeta(bronzeMeta);
                        keys.add(bronzeKey);
                        break;
                    case "silver":
                        ItemStack silverKey = new ItemStack(Material.TRIAL_KEY);
                        silverKey.setAmount(amount);
                        ItemMeta silverMeta = silverKey.getItemMeta();
                        silverMeta.itemName(Component.text("Silver key").color(NamedTextColor.GRAY));
                        silverMeta.lore(List.of(Component.text("Use this key to open silver crates").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                        silverMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING, "silver");
                        silverKey.setItemMeta(silverMeta);
                        keys.add(silverKey);
                        break;
                    case "gold":
                        ItemStack goldKey = new ItemStack(Material.TRIAL_KEY);
                        goldKey.setAmount(amount);
                        ItemMeta goldMeta = goldKey.getItemMeta();
                        goldMeta.itemName(Component.text("Gold key").color(NamedTextColor.YELLOW));
                        goldMeta.lore(List.of(Component.text("Use this key to open gold crates").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                        goldMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING, "gold");
                        goldKey.setItemMeta(goldMeta);
                        keys.add(goldKey);
                        break;
                    case "diamond":
                        ItemStack diamondKey = new ItemStack(Material.TRIAL_KEY);
                        diamondKey.setAmount(amount);
                        ItemMeta diamondMeta = diamondKey.getItemMeta();
                        diamondMeta.itemName(Component.text("Diamond key").color(NamedTextColor.AQUA));
                        diamondMeta.lore(List.of(Component.text("Use this key to open diamond crates").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                        diamondMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING, "diamond");
                        diamondKey.setItemMeta(diamondMeta);
                        keys.add(diamondKey);
                        break;
                }
            }
            return keys;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get keys for player " + playerUUID + ": " + e.getMessage());
            return null;
        }
    }

    public boolean removeKey(UUID playerUUID, ItemStack key) {
        String keyType = key.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "key_type"), PersistentDataType.STRING);
        if (keyType != null) {
            String sql = "UPDATE player_keys SET amount = GREATEST(amount - ?, 0) WHERE player_uuid = ? AND key_type = ?";
            try (Connection connection = dataSource.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, key.getAmount());
                preparedStatement.setString(2, playerUUID.toString());
                preparedStatement.setString(3, keyType);
                int rowsAffected = preparedStatement.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove key for player " + playerUUID + ": " + e.getMessage());
            }
        }
        return false;
    }

    public boolean addVoteKey(UUID playerUUID) {
        String sql = "INSERT INTO player_keys (player_uuid, key_type, amount) VALUES (?, 'vote', 1) " +
            "ON DUPLICATE KEY UPDATE amount = amount + 1";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, playerUUID.toString());
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) return true;
            else {
                plugin.getLogger().warning("Failed to add vote key for player " + playerUUID);
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add vote key for player " + playerUUID + ": " + e.getMessage());
            return false;
        }
    }
}