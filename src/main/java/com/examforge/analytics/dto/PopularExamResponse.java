package com.examforge.analytics.dto;

import java.util.UUID;

public record PopularExamResponse(
        UUID examId,
        String examName,
        long attemptCount
) {
}
