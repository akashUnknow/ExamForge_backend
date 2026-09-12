package com.examforge.attempt.dto;

import com.examforge.question.domain.QuestionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-question review shown after an attempt is submitted. Correct
 * answers are legitimately included here - the platform rule is that
 * they're hidden only during an active attempt (see QuestionAttemptResponse),
 * not that they're hidden forever.
 */
public record QuestionReviewResponse(
        UUID questionId,
        QuestionType questionType,
        String questionText,
        String explanation,
        BigDecimal marks,
        BigDecimal negativeMarks,
        List<QuestionReviewOptionResponse> options,
        Set<UUID> selectedOptionIds,
        BigDecimal numericAnswerGiven,
        BigDecimal correctNumericAnswer,
        boolean answered,
        boolean correct,
        BigDecimal marksAwarded
) {
}
