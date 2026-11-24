package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleNote;
import ru.mirea.parking.dto.note.NoteCreateRequest;
import ru.mirea.parking.dto.note.NoteResponse;
import ru.mirea.parking.dto.note.NoteUpdateRequest;
import ru.mirea.parking.error.ConflictException;
import ru.mirea.parking.error.ForbiddenException;
import ru.mirea.parking.error.NotFoundException;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.repository.VehicleNoteRepository;
import ru.mirea.parking.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

/** Заметки к автомобилю: изменять и удалять может только автор или Администратор. */
@Service
public class VehicleNoteService {

    private static final String STALE_VERSION = "Заметку изменил другой пользователь, обновите список";

    private final VehicleNoteRepository notes;
    private final VehicleService vehicleService;
    private final UserAccountRepository users;

    public VehicleNoteService(VehicleNoteRepository notes, VehicleService vehicleService, UserAccountRepository users) {
        this.notes = notes;
        this.vehicleService = vehicleService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID vehicleId, UserPrincipal principal) {
        Vehicle vehicle = vehicleService.findVehicle(vehicleId);
        return notes.findByVehicleIdOrderByPinnedDescCreatedAtDesc(vehicle.getId()).stream()
                .map(note -> toResponse(note, canEdit(note, principal)))
                .toList();
    }

    @Transactional
    public NoteResponse create(UUID vehicleId, NoteCreateRequest body, UserPrincipal principal) {
        Vehicle vehicle = vehicleService.findVehicle(vehicleId);
        UserAccount author = users.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Учётная запись автора не найдена"));
        VehicleNote note = new VehicleNote(vehicle, author, body.body().trim(), Boolean.TRUE.equals(body.pinned()));
        return toResponse(notes.saveAndFlush(note), true);
    }

    @Transactional
    public NoteResponse update(UUID vehicleId, UUID noteId, NoteUpdateRequest body, UserPrincipal principal) {
        VehicleNote note = findEditableNote(vehicleId, noteId, principal);
        if (body.version() != note.getVersion()) {
            throw new ConflictException(STALE_VERSION);
        }
        note.setBody(body.body().trim());
        note.setPinned(body.pinned());
        return toResponse(notes.saveAndFlush(note), true);
    }

    @Transactional
    public void delete(UUID vehicleId, UUID noteId, UserPrincipal principal) {
        notes.delete(findEditableNote(vehicleId, noteId, principal));
    }

    private VehicleNote findEditableNote(UUID vehicleId, UUID noteId, UserPrincipal principal) {
        Vehicle vehicle = vehicleService.findVehicle(vehicleId);
        VehicleNote note = notes.findByIdAndVehicleId(noteId, vehicle.getId())
                .orElseThrow(() -> new NotFoundException("Заметка " + noteId + " не найдена"));
        if (!canEdit(note, principal)) {
            throw new ForbiddenException("Изменять и удалять заметку может только её автор или администратор");
        }
        return note;
    }

    private static boolean canEdit(VehicleNote note, UserPrincipal principal) {
        return note.getAuthor().getId().equals(principal.id()) || principal.roles().contains(Role.ADMIN);
    }

    private static NoteResponse toResponse(VehicleNote note, boolean canEdit) {
        return NoteResponse.of(note.getId(), note.getBody(), note.isPinned(), note.getAuthor(), note.getCreatedAt(),
                note.getUpdatedAt(), note.getVersion(), canEdit);
    }
}
