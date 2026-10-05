package ru.mirea.project.repository;

import java.util.List;
import java.util.Optional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;

import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.util.DatabaseManager;

public class RequestRepository implements CrudRepository<Request> {

    @Override
    public Request create(Request request) {
        String sql = "INSERT INTO requests (user_id, vehicle_id, spot_id, start_time, end_time, status_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.getUserId());
            statement.setLong(2, request.getVehicleId());
            statement.setLong(3, request.getSpotId());
            statement.setTimestamp(4, Timestamp.valueOf(request.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(request.getEndTime()));
            statement.setInt(6, request.getStatus().getId());
            statement.setTimestamp(7, Timestamp.valueOf(request.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setId(keys.getLong(1));
                }
            }
            return request;
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось создать заявку", err);
        }
    }

    @Override
    public List<Request> findAll() {
        String sql = "SELECT id, user_id, vehicle_id, spot_id, start_time, end_time, status_id, created_at FROM requests ORDER BY id";
        
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {
            List<Request> requests = new ArrayList<>();

            while (resultSet.next()) {
                requests.add(mapRow(resultSet));
            }
            return requests;
        } 
        catch (SQLException err) {
            throw new DataAccessException("Не удалось получить список заявок", err);
        }
    }

    @Override
    public Optional<Request> findById(long id) {
        String sql = "SELECT id, user_id, vehicle_id, spot_id, start_time, end_time, status_id, created_at FROM requests WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } 
        catch (SQLException err) {
            throw new DataAccessException("Не удалось получить заявку", err);
        }
    }

    @Override
    public void update(Request request) {
        String sql = "UPDATE requests SET user_id = ?, vehicle_id = ?, spot_id = ?, start_time = ?, end_time = ?, status_id = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.getUserId());
            statement.setLong(2, request.getVehicleId());
            statement.setLong(3, request.getSpotId());
            statement.setTimestamp(4, Timestamp.valueOf(request.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(request.getEndTime()));
            statement.setInt(6, request.getStatus().getId());
            statement.setLong(7, request.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить заявку", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM requests WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось удалить заявку", err);
        }
    }

    private Request mapRow(ResultSet rs) throws SQLException {
        int statusId = rs.getInt("status_id");
        RequestStatus status = RequestStatus.fromId(statusId)
            .orElseThrow(() -> new SQLException("Неизвестный статус заявки с id " + statusId));

        return new Request(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getLong("vehicle_id"),
            rs.getLong("spot_id"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            status,
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
