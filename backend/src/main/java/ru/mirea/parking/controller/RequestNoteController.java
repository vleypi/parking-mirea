package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.note.RequestNoteCreateRequest;
import ru.mirea.parking.dto.note.RequestNoteResponse;
import ru.mirea.parking.dto.note.RequestNoteUpdateRequest;
import ru.mirea.parking.security.UserPrincipal;
import ru.mirea.parking.service.RequestNoteService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "request notes")
@RestController
@RequestMapping("/api/requests/{requestId}/notes")
public class RequestNoteController {

    private final RequestNoteService noteService;

    public RequestNoteController(RequestNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<RequestNoteResponse> list(@PathVariable UUID requestId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.list(requestId, principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<RequestNoteResponse> create(@PathVariable UUID requestId,
                                                      @Valid @RequestBody RequestNoteCreateRequest body,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        RequestNoteResponse created = noteService.create(requestId, body, principal);
        return ResponseEntity.created(URI.create("/api/requests/" + requestId + "/notes/" + created.id())).body(created);
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestNoteResponse update(@PathVariable UUID requestId, @PathVariable UUID noteId,
                                      @Valid @RequestBody RequestNoteUpdateRequest body,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.update(requestId, noteId, body, principal);
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID requestId, @PathVariable UUID noteId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        noteService.delete(requestId, noteId, principal);
    }
}
