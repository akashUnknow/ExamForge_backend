package com.examforge.bookmark.dto;

import com.examforge.bookmark.domain.Bookmark;
import com.examforge.question.domain.QuestionType;

import java.time.Instant;
import java.util.UUID;

/**
 * A user can bookmark a question at any point, including mid-attempt
 * before the answer key would ever be shown to them - so this stays
 * answer-free by the same rule as QuestionAttemptResponse, not a
 * shortcut of question.dto.QuestionResponse.
 */
public record BookmarkResponse(
        UUID id,
        UUID questionId,
        String questionText,
        QuestionType questionType,
        Instant createdAt
) {
    public static BookmarkResponse from(Bookmark bookmark) {
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getQuestion().getId(),
                bookmark.getQuestion().getQuestionText(),
                bookmark.getQuestion().getQuestionType(),
                bookmark.getCreatedAt()
        );
    }
}
