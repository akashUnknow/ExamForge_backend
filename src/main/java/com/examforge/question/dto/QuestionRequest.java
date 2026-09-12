package com.examforge.question.dto;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QuestionRequest {

    @NotNull(message = "Topic id is required")
    private UUID topicId;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String explanation;

    @NotNull(message = "Difficulty is required")
    private ExamDifficulty difficulty;

    @NotNull(message = "Marks is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Marks must be greater than 0")
    private BigDecimal marks;

    @NotNull(message = "Negative marks is required (use 0 for none)")
    @DecimalMin(value = "0.0", message = "Negative marks cannot be negative")
    private BigDecimal negativeMarks;

    @Size(max = 10, message = "Language code must be at most 10 characters")
    private String language = "en";

    /** Required and only meaningful when questionType == NUMERIC. */
    private BigDecimal correctNumericAnswer;

    private Set<@Size(max = 50) String> tags;

    /** Required for MCQ / MULTIPLE_CHOICE / TRUE_FALSE; ignored for NUMERIC. */
    @Valid
    private List<QuestionOptionRequest> options;

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
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

    public ExamDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(ExamDifficulty difficulty) {
        this.difficulty = difficulty;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public BigDecimal getCorrectNumericAnswer() {
        return correctNumericAnswer;
    }

    public void setCorrectNumericAnswer(BigDecimal correctNumericAnswer) {
        this.correctNumericAnswer = correctNumericAnswer;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public List<QuestionOptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionRequest> options) {
        this.options = options;
    }
}
