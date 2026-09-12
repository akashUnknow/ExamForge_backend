package com.examforge.test.dto;

import com.examforge.exam.domain.ExamDifficulty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * One bucket in an auto-generate request, e.g. "20 EASY questions from
 * subject X" or "10 HARD questions from topic Y". Exactly one of subjectId
 * / topicId must be set (validated in the service, not here, since it's a
 * cross-field rule).
 */
public class AutoGenerateBucket {

    private UUID subjectId;

    private UUID topicId;

    @NotNull(message = "Difficulty is required for each bucket")
    private ExamDifficulty difficulty;

    @NotNull(message = "Count is required for each bucket")
    @Positive(message = "Count must be positive")
    private Integer count;

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public ExamDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ExamDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
