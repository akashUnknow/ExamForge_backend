package com.examforge.question.dto;

import com.examforge.question.domain.QuestionTranslation;

import java.util.UUID;

public record QuestionTranslationResponse(
        UUID id,
        String language,
        String questionText,
        String explanation
) {
    public static QuestionTranslationResponse from(QuestionTranslation translation) {
        return new QuestionTranslationResponse(
                translation.getId(), translation.getLanguage(), translation.getQuestionText(), translation.getExplanation());
    }
}
