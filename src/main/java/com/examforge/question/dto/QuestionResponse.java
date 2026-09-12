package com.examforge.question.dto;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Full, authoring-side view of a question - includes correct answers and
 * explanations. Only ever served to CONTENT_CREATOR / REVIEWER / ADMIN /
 * SUPER_ADMIN through the {@code /api/v1/questions} endpoints in this
 * module.
 *
 * IMPORTANT: this DTO must never be reused for a test-taking-facing
 * endpoint. When the Test/Attempt module (Phase 5/6) needs to serve
 * question content to a user during an active attempt, it must use a
 * separate response shape that omits {@code correct} on every option and
 * omits {@code explanation} and {@code correctNumericAnswer} entirely,
 * per the platform rule that correct answers are never exposed during an
 * active test.
 */
public record QuestionResponse(
        UUID id,
        UUID topicId,
        String topicName,
        UUID subjectId,
        String subjectName,
        UUID examId,
        QuestionType questionType,
        String questionText,
        String explanation,
        ExamDifficulty difficulty,
        BigDecimal marks,
        BigDecimal negativeMarks,
        String language,
        QuestionStatus status,
        String rejectionReason,
        BigDecimal correctNumericAnswer,
        Set<String> tags,
        List<QuestionOptionResponse> options,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionResponse from(Question question) {
        var topic = question.getTopic();
        var subject = topic.getSubject();

        return new QuestionResponse(
                question.getId(),
                topic.getId(),
                topic.getName(),
                subject.getId(),
                subject.getName(),
                subject.getExam().getId(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getExplanation(),
                question.getDifficulty(),
                question.getMarks(),
                question.getNegativeMarks(),
                question.getLanguage(),
                question.getStatus(),
                question.getRejectionReason(),
                question.getCorrectNumericAnswer(),
                question.getTags(),
                question.getOptions().stream().map(QuestionOptionResponse::from).collect(Collectors.toList()),
                question.getCreatedBy(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
