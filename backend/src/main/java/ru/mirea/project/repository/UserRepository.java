package ru.mirea.project.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.model.User;
import ru.mirea.project.util.DatabaseManager;

public class UserRepository implements CrudRepository<User> {
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (name, phone, created_at) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getPhone());
            statement.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось создать владельца", err);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, name, phone, created_at FROM users ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();

            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
            return users;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось получить список владельцев", err);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT id, name, phone, created_at FROM users WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось получить владельца", err);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET name = ?, phone = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getPhone());
            statement.setLong(3, user.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить владельца", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException err) {
            if (FOREIGN_KEY_VIOLATION.equals(err.getSQLState())) {
                throw new DataAccessException("Нельзя удалить владельца: у него есть заявки на парковку", err);
            }
            throw new DataAccessException("Не удалось удалить владельца", err);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
