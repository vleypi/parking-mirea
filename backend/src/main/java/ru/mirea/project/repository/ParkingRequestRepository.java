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
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;

public class ParkingRequestRepository implements CrudRepository<ParkingRequest> {

    @Override
    public ParkingRequest create(ParkingRequest request) {
        String sql = "INSERT INTO parking_requests (user_id, license_plate, spot_number, start_time, end_time, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.getUserId());
            statement.setString(2, request.getLicensePlate());
            statement.setInt(3, request.getSpotNumber());
            statement.setTimestamp(4, Timestamp.valueOf(request.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(request.getEndTime()));
            statement.setString(6, request.getStatus().name());
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
    public List<ParkingRequest> findAll() {
        String sql = "SELECT id, user_id, license_plate, spot_number, start_time, end_time, status, created_at FROM parking_requests ORDER BY id";
        
        try (Connection connection = DatabaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {
            List<ParkingRequest> requests = new ArrayList<>();

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
    public Optional<ParkingRequest> findById(long id) {
        String sql = "SELECT id, user_id, license_plate, spot_number, start_time, end_time, status, created_at FROM parking_requests WHERE id = ?";

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
    public void update(ParkingRequest request) {
        String sql = "UPDATE parking_requests SET user_id = ?, license_plate = ?, spot_number = ?, start_time = ?, end_time = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.getUserId());
            statement.setString(2, request.getLicensePlate());
            statement.setInt(3, request.getSpotNumber());
            statement.setTimestamp(4, Timestamp.valueOf(request.getStartTime()));
            statement.setTimestamp(5, Timestamp.valueOf(request.getEndTime()));
            statement.setString(6, request.getStatus().name());
            statement.setLong(7, request.getId());
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось обновить заявку", err);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM parking_requests WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException err) {
            throw new DataAccessException("Не удалось удалить заявку", err);
        }
    }

    private ParkingRequest mapRow(ResultSet rs) throws SQLException {
        return new ParkingRequest(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("license_plate"),
            rs.getInt("spot_number"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            RequestStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
