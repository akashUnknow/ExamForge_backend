package com.examforge.test.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TestQuestionUpdateRequest {

    private UUID sectionId;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    @NotNull(message = "Marks is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Marks must be greater than 0")
    private BigDecimal marks;

    @NotNull(message = "Negative marks is required (use 0 for none)")
    @DecimalMin(value = "0.0", message = "Negative marks cannot be negative")
    private BigDecimal negativeMarks;

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
