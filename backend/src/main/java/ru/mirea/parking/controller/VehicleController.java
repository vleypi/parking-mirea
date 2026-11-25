package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.parking.dto.BulkResponse;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.dto.vehicle.VehicleBulkRequest;
import ru.mirea.parking.dto.vehicle.VehicleCreateRequest;
import ru.mirea.parking.dto.vehicle.VehicleRequestsResponse;
import ru.mirea.parking.dto.vehicle.VehicleResponse;
import ru.mirea.parking.dto.vehicle.VehicleUpdateRequest;
import ru.mirea.parking.filter.VehicleFilter;
import ru.mirea.parking.service.VehicleBulkService;
import ru.mirea.parking.service.VehicleCsvService;
import ru.mirea.parking.service.VehicleService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Автомобили: чтение всем ролям, изменение Оператору и Администратору, окончательное удаление — Администратору. */
@Tag(name = "vehicles", description = "Автомобили владельцев")
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleBulkService bulkService;
    private final VehicleCsvService csvService;

    public VehicleController(VehicleService vehicleService, VehicleBulkService bulkService, VehicleCsvService csvService) {
        this.vehicleService = vehicleService;
        this.bulkService = bulkService;
        this.csvService = csvService;
    }

    @Operation(summary = "Список с поиском, фильтрами, сортировкой (licensePlate, brand, releaseYear, createdAt) и страницами")
    @GetMapping
    public PageResponse<VehicleResponse> search(@ModelAttribute VehicleFilter filter,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "createdAt") String sort,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return vehicleService.search(filter, page, size, sort, direction);
    }

    @Operation(summary = "Автомобили, стоявшие на парковке в период (неотменённые заявки, пересекающие период)")
    @GetMapping("/parked")
    public List<VehicleResponse> parked(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return vehicleService.parkedDuring(from, to);
    }

    @Operation(summary = "Экспорт в CSV с теми же фильтрами, что у списка")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@ModelAttribute VehicleFilter filter) {
        return csvService.export(filter);
    }

    @Operation(summary = "Проверка CSV без записи: ошибки по строкам и importToken")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportPreview importPreview(@RequestPart("file") MultipartFile file) {
        return csvService.preview(file);
    }

    @Operation(summary = "Применение проверенного файла: CREATE_ONLY или UPSERT по гос. номеру")
    @PostMapping("/import/apply")
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportResult importApply(@Valid @RequestBody ImportApplyRequest body) {
        return csvService.apply(body);
    }

    @Operation(summary = "Автомобиль по UUID")
    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable UUID id) {
        return vehicleService.get(id);
    }

    @Operation(summary = "Последние 20 заявок автомобиля для карточки")
    @GetMapping("/{id}/requests")
    public VehicleRequestsResponse requests(@PathVariable UUID id) {
        return vehicleService.requests(id);
    }

    @Operation(summary = "Добавить автомобиль")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleCreateRequest body) {
        VehicleResponse created = vehicleService.create(body);
        return ResponseEntity.created(URI.create("/api/vehicles/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить автомобиль; version из карточки, при несовпадении 409")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleUpdateRequest body) {
        return vehicleService.update(id, body);
    }

    @Operation(summary = "Перенести в архив")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public VehicleResponse archive(@PathVariable UUID id) {
        return vehicleService.archive(id);
    }

    @Operation(summary = "Восстановить из архива")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OPERATOR')")
    public VehicleResponse restore(@PathVariable UUID id) {
        return vehicleService.restore(id);
    }

    @Operation(summary = "Удалить окончательно (Администратор, PERMANENT_DELETE_ENABLED=true, нет заявок)")
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID id) {
        vehicleService.deletePermanently(id);
    }

    @Operation(summary = "Массовая операция: ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG")
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR')")
    public BulkResponse bulk(@Valid @RequestBody VehicleBulkRequest body) {
        return bulkService.apply(body);
    }
}
