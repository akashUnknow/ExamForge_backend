package com.examforge.testseries.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TestSeriesTestAttachRequest {

    @NotNull(message = "Test id is required")
    private UUID testId;

    private Integer displayOrder;

    public UUID getTestId() {
        return testId;
    }

    public void setTestId(UUID testId) {
        this.testId = testId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
