package com.examforge.attempt.dto;

import com.examforge.question.domain.QuestionOption;

import java.util.UUID;

/**
 * Option shape served during an active attempt. Deliberately omits
 * {@code correct} - this is the answer-free counterpart to
 * question.dto.QuestionOptionResponse, which must never be reused here.
 */
public record QuestionAttemptOptionResponse(
        UUID id,
        String optionText,
        Integer displayOrder
) {
    public static QuestionAttemptOptionResponse from(QuestionOption option) {
        return new QuestionAttemptOptionResponse(option.getId(), option.getOptionText(), option.getDisplayOrder());
    }
}
