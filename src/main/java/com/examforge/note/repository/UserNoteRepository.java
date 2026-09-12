package com.examforge.note.repository;

import com.examforge.note.domain.UserNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserNoteRepository extends JpaRepository<UserNote, UUID> {

    Optional<UserNote> findByUserIdAndQuestionId(UUID userId, UUID questionId);
}
