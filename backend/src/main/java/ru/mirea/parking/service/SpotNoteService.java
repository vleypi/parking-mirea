package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotNote;
import ru.mirea.parking.dto.note.NoteCreateRequest;
import ru.mirea.parking.dto.note.NoteResponse;
import ru.mirea.parking.dto.note.NoteUpdateRequest;
import ru.mirea.parking.error.ConflictException;
import ru.mirea.parking.error.ForbiddenException;
import ru.mirea.parking.error.NotFoundException;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.repository.SpotNoteRepository;
import ru.mirea.parking.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

/** Заметки к парковочному месту: изменять и удалять может только автор или Администратор. */
@Service
public class SpotNoteService {

    private static final String STALE_VERSION = "Заметку изменил другой пользователь, обновите список";

    private final SpotNoteRepository notes;
    private final SpotService spotService;
    private final UserAccountRepository users;

    public SpotNoteService(SpotNoteRepository notes, SpotService spotService, UserAccountRepository users) {
        this.notes = notes;
        this.spotService = spotService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID spotId, UserPrincipal principal) {
        Spot spot = spotService.findSpot(spotId);
        return notes.findBySpotIdOrderByPinnedDescCreatedAtDesc(spot.getId()).stream()
                .map(note -> toResponse(note, canEdit(note, principal)))
                .toList();
    }

    @Transactional
    public NoteResponse create(UUID spotId, NoteCreateRequest body, UserPrincipal principal) {
        Spot spot = spotService.findSpot(spotId);
        UserAccount author = users.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Учётная запись автора не найдена"));
        SpotNote note = new SpotNote(spot, author, body.body().trim(), Boolean.TRUE.equals(body.pinned()));
        return toResponse(notes.saveAndFlush(note), true);
    }

    @Transactional
    public NoteResponse update(UUID spotId, UUID noteId, NoteUpdateRequest body, UserPrincipal principal) {
        SpotNote note = findEditableNote(spotId, noteId, principal);
        if (body.version() != note.getVersion()) {
            throw new ConflictException(STALE_VERSION);
        }
        note.setBody(body.body().trim());
        note.setPinned(body.pinned());
        return toResponse(notes.saveAndFlush(note), true);
    }

    @Transactional
    public void delete(UUID spotId, UUID noteId, UserPrincipal principal) {
        notes.delete(findEditableNote(spotId, noteId, principal));
    }

    private SpotNote findEditableNote(UUID spotId, UUID noteId, UserPrincipal principal) {
        Spot spot = spotService.findSpot(spotId);
        SpotNote note = notes.findByIdAndSpotId(noteId, spot.getId())
                .orElseThrow(() -> new NotFoundException("Заметка " + noteId + " не найдена"));
        if (!canEdit(note, principal)) {
            throw new ForbiddenException("Изменять и удалять заметку может только её автор или администратор");
        }
        return note;
    }

    private static boolean canEdit(SpotNote note, UserPrincipal principal) {
        return note.getAuthor().getId().equals(principal.id()) || principal.roles().contains(Role.ADMIN);
    }

    private static NoteResponse toResponse(SpotNote note, boolean canEdit) {
        return NoteResponse.of(note.getId(), note.getBody(), note.isPinned(), note.getAuthor(), note.getCreatedAt(),
                note.getUpdatedAt(), note.getVersion(), canEdit);
    }
}
