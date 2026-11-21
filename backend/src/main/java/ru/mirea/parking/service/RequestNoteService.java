package ru.mirea.parking.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestNote;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.dto.note.RequestNoteCreateRequest;
import ru.mirea.parking.dto.note.RequestNoteResponse;
import ru.mirea.parking.dto.note.RequestNoteUpdateRequest;
import ru.mirea.parking.repository.RequestNoteRepository;
import ru.mirea.parking.repository.RequestRepository;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class RequestNoteService {

    private static final String STALE_VERSION = "Заметку изменил другой пользователь, обновите список";

    private final RequestNoteRepository notes;
    private final RequestRepository requests;
    private final UserAccountRepository users;

    public RequestNoteService(RequestNoteRepository notes, RequestRepository requests, UserAccountRepository users) {
        this.notes = notes;
        this.requests = requests;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RequestNoteResponse> list(UUID requestId, UserPrincipal principal) {
        Request request = findRequest(requestId);
        return notes.findByRequestIdOrderByPinnedDescCreatedAtDesc(request.getId()).stream()
                .map(note -> RequestNoteResponse.from(note, canEdit(note, principal)))
                .toList();
    }

    @Transactional
    public RequestNoteResponse create(UUID requestId, RequestNoteCreateRequest body, UserPrincipal principal) {
        Request request = findRequest(requestId);
        UserAccount author = users.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Требуется вход в систему"));
        RequestNote note = new RequestNote(request, author, body.body().trim(), Boolean.TRUE.equals(body.pinned()));
        return RequestNoteResponse.from(notes.saveAndFlush(note), true);
    }

    @Transactional
    public RequestNoteResponse update(UUID requestId, UUID noteId, RequestNoteUpdateRequest body, UserPrincipal principal) {
        RequestNote note = findEditableNote(requestId, noteId, principal);
        if (body.version() != note.getVersion()) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
        note.setBody(body.body().trim());
        note.setPinned(body.pinned());
        try {
            return RequestNoteResponse.from(notes.saveAndFlush(note), true);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
    }

    @Transactional
    public void delete(UUID requestId, UUID noteId, UserPrincipal principal) {
        notes.delete(findEditableNote(requestId, noteId, principal));
    }

    private RequestNote findEditableNote(UUID requestId, UUID noteId, UserPrincipal principal) {
        Request request = findRequest(requestId);
        RequestNote note = notes.findByIdAndRequestId(noteId, request.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Заметка " + noteId + " не найдена"));
        if (!canEdit(note, principal)) {
            throw new ResponseStatusException(FORBIDDEN, "Изменять и удалять заметку может только её автор или администратор");
        }
        return note;
    }

    private static boolean canEdit(RequestNote note, UserPrincipal principal) {
        return note.getAuthor().getId().equals(principal.id()) || principal.roles().contains(Role.ADMIN);
    }

    private Request findRequest(UUID id) {
        return requests.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Заявка " + id + " не найдена"));
    }
}
