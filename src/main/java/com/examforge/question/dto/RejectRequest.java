package com.examforge.question.dto;

import jakarta.validation.constraints.Size;

public class RejectRequest {

    @Size(max = 1000, message = "Reason must be at most 1000 characters")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
