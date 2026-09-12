package com.examforge.analytics.dto;

import java.util.UUID;

public record PopularTestResponse(
        UUID testId,
        String testName,
        long attemptCount
) {
}
