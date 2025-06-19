package org.leafhold.skyBlock.utils;

import org.leafhold.skyBlock.SkyBlock;

import java.sql.*;

public class DatabaseManager {
    private Connection connection;

    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + SkyBlock.getInstance().getDataFolder() + "/skyblock.db");
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS islands (" +
                "uuid UUID PRIMARY KEY," +
                "owner UUID NOT NULL," +
                "name TEXT NOT NULL," +
                "world TEXT NOT NULL," +
                "public BOOLEAN NOT NULL DEFAULT false," +
                "x INTEGER NOT NULL," +
                "z INTEGER NOT NULL);" +

                "CREATE TABLE IF NOT EXISTS island_members (" +
                "island_uuid UUID NOT NULL REFERENCES islands(uuid)," +
                "member_uuid UUID NOT NULL," +
                "role TEXT NOT NULL DEFAULT 'member'," +
                "PRIMARY KEY (island_uuid, member_uuid));";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
    }
}
