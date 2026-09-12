package com.examforge.attempt.dto;

import com.examforge.attempt.domain.AttemptStatus;
import com.examforge.attempt.domain.TestAttempt;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponse(
        UUID id,
        UUID testId,
        String testName,
        Integer durationMinutes,
        Instant startedAt,
        Instant expiresAt,
        AttemptStatus status
) {
    public static AttemptResponse from(TestAttempt attempt) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getName(),
                attempt.getTest().getDurationMinutes(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getStatus()
        );
    }
}
