package ru.mirea.project.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.util.DatabaseManager;

public class VehicleRepository implements CrudRepository<Vehicle> {
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Override
    public Vehicle create(Vehicle vehicle) {
        String sql = "INSERT INTO vehicles (user_id, license_plate, brand, model) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, vehicle.getUserId());
            statement.setString(2, vehicle.getLicensePlate());
            statement.setString(3, vehicle.getBrand());
            statement.setString(4, vehicle.getModel());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    vehicle.setId(keys.getLong(1));
                }
            }
            return vehicle;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось создать автомобиль", err);
        }
    }

    @Override
    public List<Vehicle> findAll() {
        String sql = "SELECT id, user_id, license_plate, brand, model FROM vehicles ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Vehicle> vehicles = new ArrayList<>();

            while (resultSet.next()) {
                vehicles.add(mapRow(resultSet));
            }
            return vehicles;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось получить список автомобилей", err);
        }
    }

    @Override
    public Optional<Vehicle> findById(long id) {
        String sql = "SELECT id, user_id, license_plate, brand, model FROM vehicles WHERE id = ?";

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
            throw new DataAccessException("Не удалось получить автомобиль", err);
        }
    }

    @Override
    public void update(Vehicle vehicle) {
        String sql = "UPDATE vehicles SET user_id = ?, license_plate = ?, brand = ?, model = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, vehicle.getUserId());
            statement.setString(2, vehicle.getLicensePlate());
            statement.setString(3, vehicle.getBrand());
            statement.setString(4, vehicle.getModel());
            statement.setLong(5, vehicle.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить автомобиль", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM vehicles WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException err) {
            if (FOREIGN_KEY_VIOLATION.equals(err.getSQLState())) {
                throw new DataAccessException("Нельзя удалить автомобиль: на него оформлены заявки на парковку", err);
            }
            throw new DataAccessException("Не удалось удалить автомобиль", err);
        }
    }

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        return new Vehicle(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("license_plate"),
            rs.getString("brand"),
            rs.getString("model")
        );
    }
}
