package ru.mirea.parking.controller;

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
import ru.mirea.parking.dto.owner.OwnerResponse;
import ru.mirea.parking.dto.owner.OwnerUpdateRequest;
import ru.mirea.parking.filter.OwnerFilter;
import ru.mirea.parking.service.OwnerBulkService;
import ru.mirea.parking.service.OwnerService;

import java.net.URI;
import java.util.UUID;

/** Владельцы: чтение всем ролям, изменение Оператору и Администратору. */
@Tag(name = "owners")
@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    private final OwnerService ownerService;
    private final OwnerBulkService bulkService;

    public OwnerController(OwnerService ownerService, OwnerBulkService bulkService) {
        this.ownerService = ownerService;
        this.bulkService = bulkService;
    }

    @GetMapping
    public PageResponse<OwnerResponse> search(@ModelAttribute OwnerFilter filter,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "createdAt") String sort,
                                              @RequestParam(defaultValue = "desc") String direction) {
        return ownerService.search(filter, page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public OwnerResponse get(@PathVariable UUID id) {
        return ownerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OwnerResponse> create(@Valid @RequestBody OwnerCreateRequest body) {
        OwnerResponse created = ownerService.create(body);
        return ResponseEntity.created(URI.create("/api/owners/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse update(@PathVariable UUID id, @Valid @RequestBody OwnerUpdateRequest body) {
        return ownerService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse archive(@PathVariable UUID id) {
        return ownerService.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerResponse restore(@PathVariable UUID id) {
        return ownerService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID id) {
        ownerService.deletePermanently(id);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerBulkResponse bulk(@Valid @RequestBody OwnerBulkRequest body) {
        return bulkService.apply(body);
    }
}
