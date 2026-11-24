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
import ru.mirea.parking.dto.spot.SpotBulkRequest;
import ru.mirea.parking.dto.spot.SpotCreateRequest;
import ru.mirea.parking.dto.spot.SpotRequestsResponse;
import ru.mirea.parking.dto.spot.SpotResponse;
import ru.mirea.parking.dto.spot.SpotUpdateRequest;
import ru.mirea.parking.filter.SpotFilter;
import ru.mirea.parking.service.SpotBulkService;
import ru.mirea.parking.service.SpotCsvService;
import ru.mirea.parking.service.SpotService;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Парковочные места: чтение всем ролям, изменение Оператору и Администратору, окончательное удаление — Администратору. */
@Tag(name = "spots", description = "Парковочные места")
@RestController
@RequestMapping("/api/spots")
public class SpotController {

    private final SpotService spotService;
    private final SpotBulkService bulkService;
    private final SpotCsvService csvService;

    public SpotController(SpotService spotService, SpotBulkService bulkService, SpotCsvService csvService) {
        this.spotService = spotService;
        this.bulkService = bulkService;
        this.csvService = csvService;
    }

    @Operation(summary = "Список с поиском, фильтрами, сортировкой (spotNumber, hourlyRate, zone, createdAt) и страницами")
    @GetMapping
    public PageResponse<SpotResponse> search(@ModelAttribute SpotFilter filter,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "spotNumber") String sort,
                                                @RequestParam(defaultValue = "asc") String direction) {
        return spotService.search(filter, page, size, sort, direction);
    }

    @Operation(summary = "Свободные места на период: работающие, без новой или подтверждённой заявки в периоде")
    @GetMapping("/free")
    public List<SpotResponse> free(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return spotService.freeDuring(from, to);
    }

    @Operation(summary = "Экспорт в CSV с теми же фильтрами, что у списка")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@ModelAttribute SpotFilter filter) {
        return csvService.export(filter);
    }

    @Operation(summary = "Проверка CSV без записи: ошибки по строкам и importToken")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportPreview importPreview(@RequestPart("file") MultipartFile file) {
        return csvService.preview(file);
    }

    @Operation(summary = "Применение проверенного файла: CREATE_ONLY или UPSERT по номеру места")
    @PostMapping("/import/apply")
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportResult importApply(@Valid @RequestBody ImportApplyRequest body) {
        return csvService.apply(body);
    }

    @Operation(summary = "Место по UUID")
    @GetMapping("/{id}")
    public SpotResponse get(@PathVariable UUID id) {
        return spotService.get(id);
    }

    @Operation(summary = "Последние 20 заявок на место для карточки")
    @GetMapping("/{id}/requests")
    public SpotRequestsResponse requests(@PathVariable UUID id) {
        return spotService.requests(id);
    }

    @Operation(summary = "Добавить место")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<SpotResponse> create(@Valid @RequestBody SpotCreateRequest body) {
        SpotResponse created = spotService.create(body);
        return ResponseEntity.created(URI.create("/api/spots/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить место; version из карточки, при несовпадении 409")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public SpotResponse update(@PathVariable UUID id, @Valid @RequestBody SpotUpdateRequest body) {
        return spotService.update(id, body);
    }

    @Operation(summary = "Перенести в архив (409, если есть незавершённая новая или подтверждённая заявка)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public SpotResponse archive(@PathVariable UUID id) {
        return spotService.archive(id);
    }

    @Operation(summary = "Восстановить из архива")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OPERATOR')")
    public SpotResponse restore(@PathVariable UUID id) {
        return spotService.restore(id);
    }

    @Operation(summary = "Удалить окончательно (Администратор, PERMANENT_DELETE_ENABLED=true, нет заявок)")
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID id) {
        spotService.deletePermanently(id);
    }

    @Operation(summary = "Массовая операция: ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG")
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR')")
    public BulkResponse bulk(@Valid @RequestBody SpotBulkRequest body) {
        return bulkService.apply(body);
    }
}
