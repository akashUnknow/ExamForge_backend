package com.examforge.note.dto;

import com.examforge.note.domain.UserNote;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        UUID questionId,
        String noteText,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(UserNote note) {
        return new NoteResponse(
                note.getId(), note.getQuestion().getId(), note.getNoteText(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
