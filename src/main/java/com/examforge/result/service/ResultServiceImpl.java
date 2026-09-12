package com.examforge.result.service;

import com.examforge.attempt.repository.TestAttemptRepository;
import com.examforge.common.exception.ForbiddenException;
import com.examforge.common.security.SecurityUtils;
import com.examforge.result.dto.AttemptSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final TestAttemptRepository attemptRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AttemptSummaryResponse> getMyAttempts(Pageable pageable) {
        return attemptRepository.findByUserIdOrderByStartedAtDesc(currentUserId(), pageable)
                .map(AttemptSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttemptSummaryResponse> getMyAttemptsForTest(UUID testId, Pageable pageable) {
        return attemptRepository.findByUserIdAndTestIdOrderByStartedAtDesc(currentUserId(), testId, pageable)
                .map(AttemptSummaryResponse::from);
    }

    private UUID currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ForbiddenException("Authentication is required"));
    }
}
