package com.examforge.attempt.dto;

import com.examforge.question.domain.QuestionOption;

import java.util.UUID;

public record QuestionReviewOptionResponse(
        UUID id,
        String optionText,
        boolean correct,
        Integer displayOrder
) {
    public static QuestionReviewOptionResponse from(QuestionOption option) {
        return new QuestionReviewOptionResponse(
                option.getId(), option.getOptionText(), option.isCorrect(), option.getDisplayOrder());
    }
}
