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
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.util.DatabaseManager;

public class SpotRepository implements CrudRepository<Spot> {
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Override
    public Spot create(Spot spot) {
        String sql = "INSERT INTO spots (spot_number, spot_type_id, hourly_rate) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, spot.getSpotNumber());
            statement.setInt(2, spot.getSpotType().getId());
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
    public List<Spot> findAll() {
        String sql = "SELECT id, spot_number, spot_type_id, hourly_rate FROM spots ORDER BY spot_number";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Spot> spots = new ArrayList<>();

            while (resultSet.next()) {
                spots.add(mapRow(resultSet));
            }
            return spots;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось получить список парковочных мест", err);
        }
    }

    @Override
    public Optional<Spot> findById(long id) {
        String sql = "SELECT id, spot_number, spot_type_id, hourly_rate FROM spots WHERE id = ?";

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
    public void update(Spot spot) {
        String sql = "UPDATE spots SET spot_number = ?, spot_type_id = ?, hourly_rate = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, spot.getSpotNumber());
            statement.setInt(2, spot.getSpotType().getId());
            statement.setBigDecimal(3, spot.getHourlyRate());
            statement.setLong(4, spot.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить парковочное место", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM spots WHERE id = ?";

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

    private Spot mapRow(ResultSet rs) throws SQLException {
        int spotTypeId = rs.getInt("spot_type_id");
        SpotType spotType = SpotType.fromId(spotTypeId)
            .orElseThrow(() -> new SQLException("Неизвестный тип места с id " + spotTypeId));

        return new Spot(
            rs.getLong("id"),
            rs.getInt("spot_number"),
            spotType,
            rs.getBigDecimal("hourly_rate")
        );
    }
}
