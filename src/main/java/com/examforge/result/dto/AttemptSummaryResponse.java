package com.examforge.result.dto;

import com.examforge.attempt.domain.AttemptStatus;
import com.examforge.attempt.domain.TestAttempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AttemptSummaryResponse(
        UUID attemptId,
        UUID testId,
        String testName,
        AttemptStatus status,
        BigDecimal score,
        Instant startedAt,
        Instant submittedAt
) {
    public static AttemptSummaryResponse from(TestAttempt attempt) {
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getName(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt()
        );
    }
}
