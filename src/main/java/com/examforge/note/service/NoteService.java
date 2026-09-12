package com.examforge.note.service;

import com.examforge.note.dto.NoteResponse;

import java.util.Optional;
import java.util.UUID;

public interface NoteService {

    Optional<NoteResponse> getMyNote(UUID questionId);

    /** Creates or updates (upsert) the current user's single note for this question. */
    NoteResponse saveMyNote(UUID questionId, String noteText);

    void deleteMyNote(UUID questionId);
}
