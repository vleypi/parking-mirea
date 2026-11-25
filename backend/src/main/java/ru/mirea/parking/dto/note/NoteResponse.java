package ru.mirea.parking.dto.note;

import ru.mirea.parking.domain.UserAccount;

import java.time.LocalDateTime;
import java.util.UUID;

/** Заметка в ответах API; формат совпадает с заметками заявки и владельца, web использует общий блок. */
public record NoteResponse(
        UUID id,
        String body,
        boolean pinned,
        RequestNoteResponse.Author author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version,
        boolean canEdit) {

    public static NoteResponse of(UUID id, String body, boolean pinned, UserAccount author, LocalDateTime createdAt,
                                  LocalDateTime updatedAt, long version, boolean canEdit) {
        return new NoteResponse(id, body, pinned, new RequestNoteResponse.Author(author.getId(), author.getDisplayName()),
                createdAt, updatedAt, version, canEdit);
    }
}
