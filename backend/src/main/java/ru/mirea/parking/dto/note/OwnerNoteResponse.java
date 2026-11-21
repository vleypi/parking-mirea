package ru.mirea.parking.dto.note;

import ru.mirea.parking.domain.OwnerNote;

import java.time.LocalDateTime;
import java.util.UUID;

/** Заметка к владельцу. Формат совпадает с заметками к заявке, поэтому web-клиент использует общий блок заметок. */
public record OwnerNoteResponse(
        UUID id,
        String body,
        boolean pinned,
        RequestNoteResponse.Author author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version,
        boolean canEdit) {

    public static OwnerNoteResponse from(OwnerNote note, boolean canEdit) {
        return new OwnerNoteResponse(note.getId(), note.getBody(), note.isPinned(),
                new RequestNoteResponse.Author(note.getAuthor().getId(), note.getAuthor().getDisplayName()),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion(), canEdit);
    }
}
