package org.leafhold.lhSkyBlock.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class DatabaseManager {
    private static HikariConfig hikariConfig;
    private static HikariDataSource dataSource;
    private static FileConfiguration config;
    private static lhSkyBlock plugin;

    public DatabaseManager(lhSkyBlock plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        return new DatabaseManager(lhSkyBlock.getInstance());
    }

    public static Connection getConnection() {
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
            "island_index INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            "uuid CHAR(36) NOT NULL UNIQUE," +
            "owner CHAR(36) NOT NULL," +
            "name TEXT NOT NULL," +
            "world TEXT NOT NULL," +
            "is_public BOOLEAN NOT NULL DEFAULT false," +
            "UNIQUE (world, island_index)" +
            ");";
        String memberTable =
            "CREATE TABLE IF NOT EXISTS island_members (" +
            "island_uuid CHAR(36) NOT NULL," +
            "member_uuid CHAR(36) NOT NULL," +
            "role TEXT NOT NULL DEFAULT 'member'," +
            "PRIMARY KEY (island_uuid, member_uuid)," +
            "FOREIGN KEY (island_uuid) REFERENCES islands(uuid));";
        
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
        // Integer islandIndex = 0;

        sql = "INSERT INTO islands (uuid, owner, name, world) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID.toString());
            preparedStatement.setString(2, ownerUUID.toString());
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, world);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
        return new Object[] { islandUUID };
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
        String sql = "SELECT uuid, owner, name, is_public FROM islands WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, islandUUID.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                String owner = resultSet.getString("owner");
                String name = resultSet.getString("name");
                boolean isPublic = resultSet.getBoolean("is_public");
                return new Object[] { uuid, owner, name, isPublic };
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
        String sql = "UPDATE is_islands SET public = NOT is_public WHERE uuid = ?";
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
}