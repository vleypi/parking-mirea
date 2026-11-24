package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.reference.ActiveRequest;
import ru.mirea.parking.dto.reference.ReferencesResponse;
import ru.mirea.parking.dto.reference.SpotTypeRequest;
import ru.mirea.parking.dto.reference.SpotTypeResponse;
import ru.mirea.parking.service.ReferenceService;

import java.util.List;

/** Справочники: чтение всем ролям, изменение типов мест только Администратору. */
@Tag(name = "references", description = "Справочники для форм и фильтров")
@RestController
@RequestMapping("/api/references")
public class ReferenceController {

    private final ReferenceService references;

    public ReferenceController(ReferenceService references) {
        this.references = references;
    }

    @Operation(summary = "Все справочники одним запросом: типы мест, статусы заявок, категории клиентов, статусы сущностей")
    @GetMapping
    public ReferencesResponse all() {
        return references.all();
    }

    @Operation(summary = "Типы парковочных мест, включая отключённые")
    @GetMapping("/spot-types")
    public List<SpotTypeResponse> spotTypes() {
        return references.spotTypes();
    }

    @Operation(summary = "Добавить тип места")
    @PostMapping("/spot-types")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SpotTypeResponse create(@Valid @RequestBody SpotTypeRequest body) {
        return references.createSpotType(body);
    }

    @Operation(summary = "Изменить код, название или порядок типа места")
    @PutMapping("/spot-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpotTypeResponse update(@PathVariable short id, @Valid @RequestBody SpotTypeRequest body) {
        return references.updateSpotType(id, body);
    }

    @Operation(summary = "Включить или отключить тип места (без физического удаления)")
    @PatchMapping("/spot-types/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public SpotTypeResponse setActive(@PathVariable short id, @Valid @RequestBody ActiveRequest body) {
        return references.setSpotTypeActive(id, body.active());
    }
}
