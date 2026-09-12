package com.examforge.bookmark.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BookmarkRequest {

    @NotNull(message = "Question id is required")
    private UUID questionId;

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }
}
