package com.oceanviewresort.util;

import com.oceanviewresort.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseConnection {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Properties props = new Properties();
                props.load(DatabaseConnection.class
                        .getClassLoader()
                        .getResourceAsStream("application.properties"));

                connection = DriverManager.getConnection(
                        props.getProperty("db.url"),
                        props.getProperty("db.username"),
                        props.getProperty("db.password")
                );

            } catch (Exception e) {
                throw new DatabaseException("Database connection failed", e);
            }
        }
        return connection;
    }
}
