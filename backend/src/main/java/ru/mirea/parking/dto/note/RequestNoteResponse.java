package ru.mirea.parking.dto.note;

import ru.mirea.parking.domain.RequestNote;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestNoteResponse(
        UUID id,
        String body,
        boolean pinned,
        Author author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version,
        boolean canEdit) {

    public record Author(UUID id, String displayName) {
    }

    public static RequestNoteResponse from(RequestNote note, boolean canEdit) {
        return new RequestNoteResponse(note.getId(), note.getBody(), note.isPinned(),
                new Author(note.getAuthor().getId(), note.getAuthor().getDisplayName()),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion(), canEdit);
    }
}
