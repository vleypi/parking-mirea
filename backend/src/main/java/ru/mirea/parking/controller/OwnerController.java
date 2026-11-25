package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.owner.OwnerBulkRequest;
import ru.mirea.parking.dto.owner.OwnerBulkResponse;
import ru.mirea.parking.dto.owner.OwnerCreateRequest;
import ru.mirea.parking.dto.owner.OwnerRelatedResponse;
import ru.mirea.parking.dto.owner.OwnerResponse;
import ru.mirea.parking.dto.owner.OwnerUpdateRequest;
import ru.mirea.parking.filter.OwnerFilter;
import ru.mirea.parking.service.OwnerBulkService;
import ru.mirea.parking.service.OwnerRelatedService;
import ru.mirea.parking.service.OwnerService;

import java.net.URI;
import java.util.UUID;

/** Владельцы: чтение всем ролям, изменение Оператору и Администратору. */
@Tag(name = "owners", description = "Владельцы (клиенты парковки)")
@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    private final OwnerService ownerService;
    private final OwnerBulkService bulkService;
    private final OwnerRelatedService relatedService;

    public OwnerController(OwnerService ownerService, OwnerBulkService bulkService, OwnerRelatedService relatedService) {
        this.ownerService = ownerService;
        this.bulkService = bulkService;
        this.relatedService = relatedService;
    }

    @Operation(summary = "Список с поиском, фильтрами, сортировкой и страницами")
    @GetMapping
    public PageResponse<OwnerResponse> search(@ModelAttribute OwnerFilter filter,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "createdAt") String sort,
                                              @RequestParam(defaultValue = "desc") String direction) {
        return ownerService.search(filter, page, size, sort, direction);
    }

    @Operation(summary = "Владелец по UUID")
    @GetMapping("/{id}")
    public OwnerResponse get(@PathVariable UUID id) {
        return ownerService.get(id);
    }

    /** Машины владельца и его последние 20 заявок для карточки. */
    @Operation(summary = "Автомобили и последние заявки владельца для карточки")
    @GetMapping("/{id}/related")
    public OwnerRelatedResponse related(@PathVariable UUID id) {
        return relatedService.related(id);
    }

    @Operation(summary = "Добавить владельца")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OwnerResponse> create(@Valid @RequestBody OwnerCreateRequest body) {
        OwnerResponse created = ownerService.create(body);
        return ResponseEntity.created(URI.create("/api/owners/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить владельца; version из карточки, при несовпадении 409")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse update(@PathVariable UUID id, @Valid @RequestBody OwnerUpdateRequest body) {
        return ownerService.update(id, body);
    }

    @Operation(summary = "Перенести в архив")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse archive(@PathVariable UUID id) {
        return ownerService.archive(id);
    }

    @Operation(summary = "Восстановить из архива")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse restore(@PathVariable UUID id) {
        return ownerService.restore(id);
    }

    @Operation(summary = "Удалить окончательно (Администратор, PERMANENT_DELETE_ENABLED=true)")
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID id) {
        ownerService.deletePermanently(id);
    }

    @Operation(summary = "Массовая операция над владельцами")
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerBulkResponse bulk(@Valid @RequestBody OwnerBulkRequest body) {
        return bulkService.apply(body);
    }
}
