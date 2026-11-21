package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.request.RequestCreateRequest;
import ru.mirea.parking.dto.request.RequestResponse;
import ru.mirea.parking.dto.request.RequestUpdateRequest;
import ru.mirea.parking.service.RequestService;

import java.net.URI;
import java.util.UUID;

@Tag(name = "requests")
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/{id}")
    public RequestResponse get(@PathVariable UUID id) {
        return requestService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody RequestCreateRequest body) {
        RequestResponse created = requestService.create(body);
        return ResponseEntity.created(URI.create("/api/requests/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestResponse update(@PathVariable UUID id, @Valid @RequestBody RequestUpdateRequest body) {
        return requestService.update(id, body);
    }
}
