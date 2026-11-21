package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.audit.AuditEventResponse;
import ru.mirea.parking.dto.audit.AuditTypesResponse;
import ru.mirea.parking.filter.AuditFilter;
import ru.mirea.parking.service.AuditService;

@Tag(name = "admin audit")
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AuditEventResponse> search(@ModelAttribute AuditFilter filter,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "occurredAt") String sort,
                                                   @RequestParam(defaultValue = "desc") String direction) {
        return auditService.search(filter, page, size, sort, direction);
    }

    @GetMapping("/types")
    public AuditTypesResponse types() {
        return AuditTypesResponse.all();
    }
}
