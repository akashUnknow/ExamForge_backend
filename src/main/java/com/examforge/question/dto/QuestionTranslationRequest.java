package com.examforge.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionTranslationRequest {

    @NotBlank(message = "Language is required")
    @Size(max = 10, message = "Language code must be at most 10 characters")
    private String language;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String explanation;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
