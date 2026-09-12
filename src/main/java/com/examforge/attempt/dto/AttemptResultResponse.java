package com.examforge.attempt.dto;

import com.examforge.attempt.domain.AttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptResultResponse(
        UUID attemptId,
        UUID testId,
        String testName,
        AttemptStatus status,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal percentage,
        BigDecimal accuracy,
        int correctCount,
        int incorrectCount,
        int unansweredCount,
        int timeSpentSeconds,
        Instant submittedAt,
        List<QuestionReviewResponse> questions
) {
}
