package org.leafhold.skyBlock.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.leafhold.skyBlock.SkyBlock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

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
        FileConfiguration config = SkyBlock.getInstance().getConfig();
        String host = config.getString("db.host");
        int port = config.getInt("db.port");
        String database = config.getString("db.database");
        String user = config.getString("db.user");
        String password = config.getString("db.password");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            SkyBlock.getInstance().getLogger().severe("MySQL connector not found! Please add it to your server's libraries.");
            throw new SQLException("MySQL driver not found");
        }

        try {
            connection = DriverManager.getConnection(url, user, password);
            SkyBlock.getInstance().getLogger().info("Connected to the database successfully!");
            createTable();
        } catch (SQLException e) {
            SkyBlock.getInstance().getLogger().severe("Could not connect to the database! Please check your configuration.");
        }   
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                SkyBlock.getInstance().getLogger().info("Disconnected from the database successfully!");
            } catch (SQLException e) {
                SkyBlock.getInstance().getLogger().severe("Failed to disconnect from the database: " + e.getMessage());
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
            "x INTEGER NOT NULL," +
            "z INTEGER NOT NULL);";
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
}
