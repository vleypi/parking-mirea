package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import ru.mirea.parking.dto.note.OwnerNoteCreateRequest;
import ru.mirea.parking.dto.note.OwnerNoteResponse;
import ru.mirea.parking.dto.note.OwnerNoteUpdateRequest;
import ru.mirea.parking.security.UserPrincipal;
import ru.mirea.parking.service.OwnerNoteService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "owner notes", description = "Заметки к владельцу")
@RestController
@RequestMapping("/api/owners/{ownerId}/notes")
public class OwnerNoteController {

    private final OwnerNoteService noteService;

    public OwnerNoteController(OwnerNoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "Заметки владельца: закреплённые первыми, затем новые")
    @GetMapping
    public List<OwnerNoteResponse> list(@PathVariable UUID ownerId, @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.list(ownerId, principal);
    }

    @Operation(summary = "Добавить заметку")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<OwnerNoteResponse> create(@PathVariable UUID ownerId,
                                                    @Valid @RequestBody OwnerNoteCreateRequest body,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        OwnerNoteResponse created = noteService.create(ownerId, body, principal);
        return ResponseEntity.created(URI.create("/api/owners/" + ownerId + "/notes/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить заметку (автор или Администратор)")
    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public OwnerNoteResponse update(@PathVariable UUID ownerId, @PathVariable UUID noteId,
                                    @Valid @RequestBody OwnerNoteUpdateRequest body,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.update(ownerId, noteId, body, principal);
    }

    @Operation(summary = "Удалить заметку (автор или Администратор)")
    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID ownerId, @PathVariable UUID noteId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        noteService.delete(ownerId, noteId, principal);
    }
}
