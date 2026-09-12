package com.examforge.attempt.domain;

public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    AUTO_SUBMITTED,
    /** Reserved for a future explicit "abandon" action - not reachable by any endpoint in this phase. */
    ABANDONED
}
