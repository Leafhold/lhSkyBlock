package org.leafhold.lhSkyBlock.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.leafhold.lhSkyBlock.lhSkyBlock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void connect() throws SQLException {
        FileConfiguration config = lhSkyBlock.getInstance().getConfig();
        String host = config.getString("db.host");
        int port = config.getInt("db.port");
        String database = config.getString("db.database");
        String user = config.getString("db.user");
        String password = config.getString("db.password");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            lhSkyBlock.getInstance().getLogger().severe("MySQL connector not found! Please add it to your server's libraries.");
            throw new SQLException("MySQL driver not found");
        }

        try {
            connection = DriverManager.getConnection(url, user, password);
            lhSkyBlock.getInstance().getLogger().info("Connected to the database successfully!");
            createTable();
        } catch (SQLException e) {
            lhSkyBlock.getInstance().getLogger().severe("Could not connect to the database! Please check your configuration.");
        }   
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                lhSkyBlock.getInstance().getLogger().info("Disconnected from the database successfully!");
            } catch (SQLException e) {
                lhSkyBlock.getInstance().getLogger().severe("Failed to disconnect from the database: " + e.getMessage());
            }
        }
    }

    private void createTable() throws SQLException {
        String islandTable = "CREATE TABLE IF NOT EXISTS islands (" +
            "uuid UUID PRIMARY KEY," +
            "owner UUID NOT NULL," +
            "name TEXT NOT NULL," +
            "world TEXT NOT NULL," +
            "public BOOLEAN NOT NULL DEFAULT false," +
            "island_index INTEGER NOT NULL UNIQUE AUTO_INCREMENT);";
        String memberTable =
            "CREATE TABLE IF NOT EXISTS island_members (" +
            "island_uuid UUID NOT NULL REFERENCES islands(uuid)," +
            "member_uuid UUID NOT NULL," +
            "role TEXT NOT NULL DEFAULT 'member'," +
            "PRIMARY KEY (island_uuid, member_uuid)," +
            "FOREIGN KEY (island_uuid) REFERENCES islands(uuid));";
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(islandTable)) {
            preparedStatement.executeUpdate();
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(memberTable)) {
            preparedStatement.executeUpdate();
        }
    }

    public Object[] createIsland(UUID ownerUUID, String name, String world) throws SQLException {
        String sql = "SELECT uuid FROM islands WHERE owner = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, ownerUUID.toString());
            if (preparedStatement.executeQuery().next()) {
                return null;
            }
        }
        UUID islandUUID = java.util.UUID.randomUUID();
        // Integer islandIndex = 0;

        sql = "INSERT INTO islands (uuid, owner, name, world) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, islandUUID.toString());
            preparedStatement.setString(2, ownerUUID.toString());
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, world);
            preparedStatement.executeUpdate();
        }
        return new Object[] { islandUUID };
    }

    public List<Object> getIslandsByOwner(String ownerUUID) throws SQLException {
        String sql = "SELECT uuid, owner, name, public FROM islands WHERE owner = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, ownerUUID);
            var resultSet = preparedStatement.executeQuery();
            List<Object> islands = new ArrayList<>();
            while (resultSet.next()) {
                String islandUUID = resultSet.getString("uuid");
                String owner = resultSet.getString("owner");
                String name = resultSet.getString("name");
                boolean isPublic = resultSet.getBoolean("public");
                islands.add(new Object[] { islandUUID, owner, name, isPublic });
            }
            return islands;
        }
    }

    public Object getIslandByUUID(UUID islandUUID) throws SQLException {
        String sql = "SELECT uuid, owner, name, public FROM islands WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, islandUUID.toString());
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                String owner = resultSet.getString("owner");
                String name = resultSet.getString("name");
                boolean isPublic = resultSet.getBoolean("public");
                return new Object[] { uuid, owner, name, isPublic };
            }
        }
        return null;
                
    }

    public boolean visitorsAllowed(String islandUUID) throws SQLException {
        String sql = "SELECT public FROM islands WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, islandUUID);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getBoolean("public");
            }
            return false;
        }
    }

    public void toggleVisitors(UUID islandUUID) throws SQLException {
        String sql = "UPDATE islands SET public = NOT public WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, islandUUID.toString());
            preparedStatement.executeUpdate();
        }
    }

    public Boolean deleteIsland(Player player, String islandUUID) throws SQLException {
        String sql = "DELETE FROM islands WHERE uuid = ? AND owner = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
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