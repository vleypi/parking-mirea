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
import ru.mirea.project.model.ParkingSpot;
import ru.mirea.project.model.SpotType;
import ru.mirea.project.util.DatabaseManager;

public class ParkingSpotRepository implements CrudRepository<ParkingSpot> {
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Override
    public ParkingSpot create(ParkingSpot spot) {
        String sql = "INSERT INTO parking_spots (spot_number, spot_type, hourly_rate) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, spot.getSpotNumber());
            statement.setString(2, spot.getSpotType().name());
            statement.setBigDecimal(3, spot.getHourlyRate());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    spot.setId(keys.getLong(1));
                }
            }
            return spot;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось создать парковочное место", err);
        }
    }

    @Override
    public List<ParkingSpot> findAll() {
        String sql = "SELECT id, spot_number, spot_type, hourly_rate FROM parking_spots ORDER BY spot_number";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ParkingSpot> spots = new ArrayList<>();

            while (resultSet.next()) {
                spots.add(mapRow(resultSet));
            }
            return spots;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось получить список парковочных мест", err);
        }
    }

    @Override
    public Optional<ParkingSpot> findById(long id) {
        String sql = "SELECT id, spot_number, spot_type, hourly_rate FROM parking_spots WHERE id = ?";

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
            throw new DataAccessException("Не удалось получить парковочное место", err);
        }
    }

    @Override
    public void update(ParkingSpot spot) {
        String sql = "UPDATE parking_spots SET spot_number = ?, spot_type = ?, hourly_rate = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, spot.getSpotNumber());
            statement.setString(2, spot.getSpotType().name());
            statement.setBigDecimal(3, spot.getHourlyRate());
            statement.setLong(4, spot.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить парковочное место", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM parking_spots WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException err) {
            if (FOREIGN_KEY_VIOLATION.equals(err.getSQLState())) {
                throw new DataAccessException("Нельзя удалить место: на него оформлены заявки на парковку", err);
            }
            throw new DataAccessException("Не удалось удалить парковочное место", err);
        }
    }

    private ParkingSpot mapRow(ResultSet rs) throws SQLException {
        return new ParkingSpot(
            rs.getLong("id"),
            rs.getInt("spot_number"),
            SpotType.valueOf(rs.getString("spot_type")),
            rs.getBigDecimal("hourly_rate")
        );
    }
}
