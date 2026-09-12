package com.examforge.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TestAnalyticsResponse(
        UUID testId,
        String testName,
        long totalAttempts,
        long completedAttempts,
        long inProgressAttempts,
        BigDecimal averageScore,
        BigDecimal averageAccuracy,
        BigDecimal completionRate
) {
}
