package com.examforge.question.domain;

/**
 * Workflow: CONTENT_CREATOR drafts -> submits -> REVIEWER approves/rejects
 * -> ADMIN/SUPER_ADMIN publishes. See {@code question.service.QuestionWorkflowService}
 * for the enforced transition rules.
 */
public enum QuestionStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    PUBLISHED,
    REJECTED,
    ARCHIVED
}
