package com.examforge.question.dto;

import com.examforge.question.domain.QuestionOption;

import java.util.UUID;

public record QuestionOptionResponse(
        UUID id,
        String optionText,
        boolean correct,
        Integer displayOrder
) {
    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(), option.getOptionText(), option.isCorrect(), option.getDisplayOrder());
    }
}
