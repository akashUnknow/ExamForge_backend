package com.examforge.result.service;

import com.examforge.result.dto.AttemptSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ResultService {

    /** The current user's attempt history across all tests, most recent first. */
    Page<AttemptSummaryResponse> getMyAttempts(Pageable pageable);

    /** The current user's attempts on one specific test - useful for retake history. */
    Page<AttemptSummaryResponse> getMyAttemptsForTest(UUID testId, Pageable pageable);
}
