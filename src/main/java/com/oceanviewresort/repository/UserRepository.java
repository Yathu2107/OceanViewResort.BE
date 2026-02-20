package com.oceanviewresort.repository;

import com.oceanviewresort.exception.DatabaseException;
import com.oceanviewresort.model.User;
import com.oceanviewresort.util.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                return user;
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("User fetch failed", e);
        }
    }
}
