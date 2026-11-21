package ru.mirea.project.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseManager {
    private static final Properties PROPERTIES = load();

    private DatabaseManager() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/db.properties")) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить db.properties", e);
        }
        return properties;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.user"),
                PROPERTIES.getProperty("db.password"));
    }
}
