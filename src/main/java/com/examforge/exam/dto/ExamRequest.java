package com.examforge.exam.dto;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class ExamRequest {

    private UUID categoryId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number of minutes")
    private Integer durationMinutes;

    @NotNull(message = "Total questions is required")
    @Positive(message = "Total questions must be positive")
    private Integer totalQuestions;

    @NotNull(message = "Maximum marks is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum marks must be greater than 0")
    private BigDecimal maximumMarks;

    @NotNull(message = "Negative marking is required (use 0 for none)")
    @DecimalMin(value = "0.0", message = "Negative marking cannot be negative")
    private BigDecimal negativeMarking;

    @NotNull(message = "Difficulty is required")
    private ExamDifficulty difficulty;

    /**
     * Optional. Defaults to DRAFT on creation if omitted.
     */
    private ExamStatus status;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public BigDecimal getMaximumMarks() {
        return maximumMarks;
    }

    public void setMaximumMarks(BigDecimal maximumMarks) {
        this.maximumMarks = maximumMarks;
    }

    public BigDecimal getNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(BigDecimal negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public ExamDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ExamDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }
}
