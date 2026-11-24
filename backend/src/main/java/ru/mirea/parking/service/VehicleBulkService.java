package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.dto.BulkResponse;
import ru.mirea.parking.dto.request.BulkAction;
import ru.mirea.parking.dto.vehicle.VehicleBulkRequest;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.repository.VehicleRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Массовые операции над автомобилями: каждая запись в своей транзакции, ошибка одной не отменяет остальные. */
@Service
public class VehicleBulkService {

    private final VehicleService vehicleService;
    private final VehicleRepository vehicles;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public VehicleBulkService(VehicleService vehicleService, VehicleRepository vehicles, AuditService audit,
                              PlatformTransactionManager transactionManager) {
        this.vehicleService = vehicleService;
        this.vehicles = vehicles;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public BulkResponse apply(VehicleBulkRequest body) {
        Consumer<Vehicle> action = toAction(body);
        List<UUID> succeeded = new ArrayList<>();
        List<BulkResponse.Failure> failed = new ArrayList<>();

        for (UUID id : new LinkedHashSet<>(body.ids())) {
            try {
                transaction.executeWithoutResult(status -> {
                    Vehicle vehicle = vehicleService.findVehicle(id);
                    action.accept(vehicle);
                    vehicles.saveAndFlush(vehicle);
                });
                succeeded.add(id);
            } catch (ResponseStatusException e) {
                failed.add(new BulkResponse.Failure(id, e.getReason()));
            }
        }
        return new BulkResponse(succeeded, failed);
    }

    private Consumer<Vehicle> toAction(VehicleBulkRequest body) {
        if (body.action() == BulkAction.CHANGE_STATUS && body.status() == null) {
            throw new BadRequestException("status", "Для смены статуса укажите status: ACTIVE, BLOCKED или DEREGISTERED");
        }
        boolean tagAction = body.action() == BulkAction.ADD_TAG || body.action() == BulkAction.REMOVE_TAG;
        if (tagAction && (body.tag() == null || body.tag().isBlank())) {
            throw new BadRequestException("tag", "Для действия с тегом укажите tag");
        }
        return switch (body.action()) {
            case ARCHIVE -> vehicleService::archive;
            case RESTORE -> vehicleService::restore;
            case CHANGE_STATUS -> vehicle -> {
                vehicleService.checkNotArchived(vehicle);
                vehicle.setStatus(body.status());
                audit.record(AuditEventType.UPDATE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                        "Автомобилю " + vehicle.getLicensePlate() + " установлен статус «" + body.status().getTitle() + "»");
            };
            case ADD_TAG -> vehicle -> {
                vehicleService.checkNotArchived(vehicle);
                String tag = VehicleService.normalizeTag(body.tag());
                vehicle.getTags().add(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                        "Автомобилю " + vehicle.getLicensePlate() + " добавлен тег «" + tag + "»");
            };
            case REMOVE_TAG -> vehicle -> {
                vehicleService.checkNotArchived(vehicle);
                String tag = VehicleService.normalizeTag(body.tag());
                vehicle.getTags().remove(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                        "У автомобиля " + vehicle.getLicensePlate() + " удалён тег «" + tag + "»");
            };
        };
    }
}
