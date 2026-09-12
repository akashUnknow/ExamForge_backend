package com.examforge.attempt.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for attempts nobody ever comes back to. The lazy check in
 * AttemptServiceImpl (checkAndAutoSubmitIfExpired) only fires when a user
 * touches their own attempt again after the deadline - if they close the
 * tab and never return, that attempt would otherwise sit IN_PROGRESS
 * forever. This job periodically finds and finalizes those.
 */
@Component
@RequiredArgsConstructor
public class AttemptSweepJob {

    private static final Logger log = LoggerFactory.getLogger(AttemptSweepJob.class);

    private final AttemptService attemptService;

    @Scheduled(fixedDelayString = "${examforge.attempt.sweep-interval-ms:60000}")
    public void sweepExpiredAttempts() {
        int count = attemptService.autoSubmitAllExpired();
        if (count > 0) {
            log.info("Attempt sweep: auto-submitted {} expired attempt(s)", count);
        }
    }
}
