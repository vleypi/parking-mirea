package ru.mirea.project.repository;

import java.util.List;
import java.util.Optional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;

public class ParkingRequestRepository implements CrudRepository<ParkingRequest> {

    @Override
    public ParkingRequest create(ParkingRequest request) {
        throw new UnsupportedOperationException("Не реализовано");
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
        catch (SQLException e) {
            throw new DataAccessException("Не удалось получить список заявок", e);
        }
    }

    @Override
    public Optional<ParkingRequest> findById(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public void update(ParkingRequest request) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException("Не реализовано");
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
