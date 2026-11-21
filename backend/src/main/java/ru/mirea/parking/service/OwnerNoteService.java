package ru.mirea.parking.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerNote;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.dto.note.OwnerNoteCreateRequest;
import ru.mirea.parking.dto.note.OwnerNoteResponse;
import ru.mirea.parking.dto.note.OwnerNoteUpdateRequest;
import ru.mirea.parking.repository.OwnerNoteRepository;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Заметки к владельцу: история с автором и временем, закреплённые первыми.
 * Изменять и удалять заметку может только её автор или Администратор.
 */
@Service
public class OwnerNoteService {

    private static final String STALE_VERSION = "Заметку изменил другой пользователь, обновите список";

    private final OwnerNoteRepository notes;
    private final OwnerService ownerService;
    private final UserAccountRepository users;

    public OwnerNoteService(OwnerNoteRepository notes, OwnerService ownerService, UserAccountRepository users) {
        this.notes = notes;
        this.ownerService = ownerService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<OwnerNoteResponse> list(UUID ownerId, UserPrincipal principal) {
        Owner owner = ownerService.findOwner(ownerId);
        return notes.findByOwnerIdOrderByPinnedDescCreatedAtDesc(owner.getId()).stream()
                .map(note -> OwnerNoteResponse.from(note, canEdit(note, principal)))
                .toList();
    }

    @Transactional
    public OwnerNoteResponse create(UUID ownerId, OwnerNoteCreateRequest body, UserPrincipal principal) {
        Owner owner = ownerService.findOwner(ownerId);
        UserAccount author = users.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Требуется вход в систему"));
        OwnerNote note = new OwnerNote(owner, author, body.body().trim(), Boolean.TRUE.equals(body.pinned()));
        return OwnerNoteResponse.from(notes.saveAndFlush(note), true);
    }

    @Transactional
    public OwnerNoteResponse update(UUID ownerId, UUID noteId, OwnerNoteUpdateRequest body, UserPrincipal principal) {
        OwnerNote note = findEditableNote(ownerId, noteId, principal);
        if (body.version() != note.getVersion()) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
        note.setBody(body.body().trim());
        note.setPinned(body.pinned());
        try {
            return OwnerNoteResponse.from(notes.saveAndFlush(note), true);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
    }

    @Transactional
    public void delete(UUID ownerId, UUID noteId, UserPrincipal principal) {
        notes.delete(findEditableNote(ownerId, noteId, principal));
    }

    private OwnerNote findEditableNote(UUID ownerId, UUID noteId, UserPrincipal principal) {
        Owner owner = ownerService.findOwner(ownerId);
        OwnerNote note = notes.findByIdAndOwnerId(noteId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Заметка " + noteId + " не найдена"));
        if (!canEdit(note, principal)) {
            throw new ResponseStatusException(FORBIDDEN, "Изменять и удалять заметку может только её автор или администратор");
        }
        return note;
    }

    private static boolean canEdit(OwnerNote note, UserPrincipal principal) {
        return note.getAuthor().getId().equals(principal.id()) || principal.roles().contains(Role.ADMIN);
    }
}
