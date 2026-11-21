package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.dto.owner.OwnerRelatedResponse;
import ru.mirea.parking.dto.owner.OwnerRelatedResponse.RequestItem;
import ru.mirea.parking.dto.owner.OwnerRelatedResponse.VehicleItem;
import ru.mirea.parking.repository.RequestRepository;
import ru.mirea.parking.repository.VehicleRepository;

import java.util.UUID;

/** Машины и заявки владельца для его карточки. Показываются и у архивного владельца. */
@Service
public class OwnerRelatedService {

    private final OwnerService ownerService;
    private final VehicleRepository vehicles;
    private final RequestRepository requests;

    public OwnerRelatedService(OwnerService ownerService, VehicleRepository vehicles, RequestRepository requests) {
        this.ownerService = ownerService;
        this.vehicles = vehicles;
        this.requests = requests;
    }

    @Transactional(readOnly = true)
    public OwnerRelatedResponse related(UUID ownerId) {
        Owner owner = ownerService.findOwner(ownerId);
        return new OwnerRelatedResponse(
                vehicles.findByOwnerIdOrderByLicensePlateAsc(owner.getId()).stream().map(VehicleItem::from).toList(),
                requests.findTop20ByOwnerIdOrderByStartTimeDescIdDesc(owner.getId()).stream().map(RequestItem::from).toList(),
                requests.countByOwnerId(owner.getId()));
    }
}
