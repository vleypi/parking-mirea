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
import ru.mirea.parking.dto.note.NoteCreateRequest;
import ru.mirea.parking.dto.note.NoteResponse;
import ru.mirea.parking.dto.note.NoteUpdateRequest;
import ru.mirea.parking.security.UserPrincipal;
import ru.mirea.parking.service.SpotNoteService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Заметки к парковочному месту: история, создание, изменение, удаление, закрепление. */
@Tag(name = "spot notes", description = "Заметки к парковочному месту")
@RestController
@RequestMapping("/api/spots/{spotId}/notes")
public class SpotNoteController {

    private final SpotNoteService noteService;

    public SpotNoteController(SpotNoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "Заметки места: закреплённые первыми, затем новые")
    @GetMapping
    public List<NoteResponse> list(@PathVariable UUID spotId, @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.list(spotId, principal);
    }

    @Operation(summary = "Добавить заметку")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<NoteResponse> create(@PathVariable UUID spotId, @Valid @RequestBody NoteCreateRequest body,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        NoteResponse created = noteService.create(spotId, body, principal);
        return ResponseEntity.created(URI.create("/api/spots/" + spotId + "/notes/" + created.id())).body(created);
    }

    @Operation(summary = "Изменить заметку (автор или Администратор)")
    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public NoteResponse update(@PathVariable UUID spotId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteUpdateRequest body, @AuthenticationPrincipal UserPrincipal principal) {
        return noteService.update(spotId, noteId, body, principal);
    }

    @Operation(summary = "Удалить заметку (автор или Администратор)")
    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasRole('OPERATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID spotId, @PathVariable UUID noteId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        noteService.delete(spotId, noteId, principal);
    }
}
