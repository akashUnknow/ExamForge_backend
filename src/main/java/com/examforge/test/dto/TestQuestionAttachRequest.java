package com.examforge.test.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TestQuestionAttachRequest {

    @NotNull(message = "Question id is required")
    private UUID questionId;

    private UUID sectionId;

    private Integer displayOrder;

    /** Optional - defaults to the question's own marks if omitted. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Marks must be greater than 0")
    private BigDecimal marks;

    /** Optional - defaults to the question's own negative marks if omitted. */
    @DecimalMin(value = "0.0", message = "Negative marks cannot be negative")
    private BigDecimal negativeMarks;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }
}
