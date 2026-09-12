package com.examforge.attempt.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public class AnswerRequest {

    @NotNull(message = "Question id is required")
    private UUID questionId;

    /** For MCQ/MULTIPLE_CHOICE/TRUE_FALSE. Leave empty/omit for NUMERIC. */
    private Set<UUID> selectedOptionIds;

    /** For NUMERIC only. */
    private BigDecimal numericAnswer;

    private boolean markedForReview = false;

    @PositiveOrZero(message = "Time spent cannot be negative")
    private Integer timeSpentSeconds = 0;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public Set<UUID> getSelectedOptionIds() {
        return selectedOptionIds;
    }

    public void setSelectedOptionIds(Set<UUID> selectedOptionIds) {
        this.selectedOptionIds = selectedOptionIds;
    }

    public BigDecimal getNumericAnswer() {
        return numericAnswer;
    }

    public void setNumericAnswer(BigDecimal numericAnswer) {
        this.numericAnswer = numericAnswer;
    }

    public boolean isMarkedForReview() {
        return markedForReview;
    }

    public void setMarkedForReview(boolean markedForReview) {
        this.markedForReview = markedForReview;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }
}
