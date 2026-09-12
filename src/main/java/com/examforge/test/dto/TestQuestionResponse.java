package com.examforge.test.dto;

import com.examforge.question.dto.QuestionResponse;
import com.examforge.test.domain.TestQuestion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Admin-only view of a test's attached question - includes the full
 * {@link QuestionResponse} (answer key and all), since only ADMIN/SUPER_ADMIN
 * manage test content. This must never be reused for a test-taking-facing
 * endpoint - see the warning on QuestionResponse itself.
 */
public record TestQuestionResponse(
        UUID id,
        UUID sectionId,
        Integer displayOrder,
        BigDecimal marks,
        BigDecimal negativeMarks,
        QuestionResponse question
) {
    public static TestQuestionResponse from(TestQuestion testQuestion) {
        return new TestQuestionResponse(
                testQuestion.getId(),
                testQuestion.getSection() != null ? testQuestion.getSection().getId() : null,
                testQuestion.getDisplayOrder(),
                testQuestion.getMarks(),
                testQuestion.getNegativeMarks(),
                QuestionResponse.from(testQuestion.getQuestion())
        );
    }
}
