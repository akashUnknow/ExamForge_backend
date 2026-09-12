package com.examforge.test.dto;

import com.examforge.test.domain.TestStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class TestPaperRequest {

    @NotNull(message = "Exam id is required")
    private UUID examId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    private String name;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be a positive number of minutes")
    private Integer durationMinutes;

    @NotNull(message = "Total marks is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total marks must be greater than 0")
    private BigDecimal totalMarks;

    @NotNull(message = "Negative marking is required (use 0 for none)")
    @DecimalMin(value = "0.0", message = "Negative marking cannot be negative")
    private BigDecimal negativeMarking;

    private boolean free = true;

    /** Required when free == false; ignored when free == true. */
    private BigDecimal price;

    /** Optional. Defaults to DRAFT on creation if omitted. */
    private TestStatus status;

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
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

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getNegativeMarking() {
        return negativeMarking;
    }

    public void setNegativeMarking(BigDecimal negativeMarking) {
        this.negativeMarking = negativeMarking;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }
}
