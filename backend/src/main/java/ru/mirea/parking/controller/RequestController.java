package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.dto.request.RequestBulkRequest;
import ru.mirea.parking.dto.request.RequestBulkResponse;
import ru.mirea.parking.dto.request.RequestCreateRequest;
import ru.mirea.parking.dto.request.RequestResponse;
import ru.mirea.parking.dto.request.RequestUpdateRequest;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.service.RequestBulkService;
import ru.mirea.parking.service.RequestCsvService;
import ru.mirea.parking.service.RequestService;

import java.net.URI;
import java.util.UUID;

@Tag(name = "requests", description = "Заявки на парковку")
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;
    private final RequestBulkService bulkService;
    private final RequestCsvService csvService;

    public RequestController(RequestService requestService, RequestBulkService bulkService, RequestCsvService csvService) {
        this.requestService = requestService;
        this.bulkService = bulkService;
        this.csvService = csvService;
    }

    @Operation(summary = "Список с поиском, фильтрами, сортировкой и страницами")
    @GetMapping
    public PageResponse<RequestResponse> search(@ModelAttribute RequestFilter filter,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "createdAt") String sort,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return requestService.search(filter, page, size, sort, direction);
    }

    @Operation(summary = "Экспорт в CSV с теми же фильтрами, что у списка")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@ModelAttribute RequestFilter filter) {
        return csvService.export(filter);
    }

    @Operation(summary = "Проверка CSV без записи: ошибки по строкам и importToken")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportPreview importPreview(@RequestPart("file") MultipartFile file) {
        return csvService.preview(file);
    }

    @Operation(summary = "Применить проверенный файл: CREATE_ONLY или UPSERT по коду заявки")
    @PostMapping("/import/apply")
    @PreAuthorize("hasRole('OPERATOR')")
    public ImportResult importApply(@Valid @RequestBody ImportApplyRequest body) {
        return csvService.apply(body);
    }

    @Operation(summary = "Заявка по UUID")
    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable UUID id) {
        return requestService.get(id);
    }

    @Operation(summary = "Оформить заявку: проверяются владелец, автомобиль, место и пересечения")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody RequestCreateRequest body) {
        RequestResponse created = requestService.create(body);
        return ResponseEntity.created(URI.create("/api/requests/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить заявку или её статус; version из карточки, при несовпадении 409")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestResponse update(@PathVariable UUID id, @Valid @RequestBody RequestUpdateRequest body) {
        return requestService.update(id, body);
    }

    @Operation(summary = "Перенести в архив")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestResponse archive(@PathVariable UUID id) {
        return requestService.archive(id);
    }

    @Operation(summary = "Восстановить из архива")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestResponse restore(@PathVariable UUID id) {
        return requestService.restore(id);
    }

    @Operation(summary = "Удалить окончательно (Администратор, PERMANENT_DELETE_ENABLED=true)")
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanently(@PathVariable UUID id) {
        requestService.deletePermanently(id);
    }

    @Operation(summary = "Массовая операция над заявками")
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestBulkResponse bulk(@Valid @RequestBody RequestBulkRequest body) {
        return bulkService.apply(body);
    }
}
