package ru.mirea.project.repository;

import java.util.List;
import java.util.Optional;

import ru.mirea.project.model.ParkingRequest;

public class ParkingRequestRepository implements CrudRepository<ParkingRequest> {

    @Override
    public ParkingRequest create(ParkingRequest request) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public List<ParkingRequest> findAll() {
        throw new UnsupportedOperationException("Не реализовано");
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
}
