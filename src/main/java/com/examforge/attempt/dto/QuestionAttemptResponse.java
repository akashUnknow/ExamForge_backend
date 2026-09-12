package com.examforge.attempt.dto;

import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The question shape served to a user during an active attempt.
 *
 * This is the answer-free DTO flagged repeatedly since Phase 4: no
 * {@code correct} field on any option (see {@link QuestionAttemptOptionResponse}),
 * no {@code explanation}, no {@code correctNumericAnswer}. Every field
 * here is safe to show someone who hasn't answered yet.
 *
 * Never construct this from question.dto.QuestionResponse or
 * test.dto.TestQuestionResponse - both legitimately carry the answer key
 * for their own (authorized, non-test-taking) audiences and must stay
 * separate from this one.
 */
public record QuestionAttemptResponse(
        UUID id,
        QuestionType questionType,
        String questionText,
        BigDecimal marks,
        BigDecimal negativeMarks,
        Integer displayOrder,
        List<QuestionAttemptOptionResponse> options
) {
    public static QuestionAttemptResponse from(Question question, Integer displayOrder, BigDecimal marks, BigDecimal negativeMarks) {
        return new QuestionAttemptResponse(
                question.getId(),
                question.getQuestionType(),
                question.getQuestionText(),
                marks,
                negativeMarks,
                displayOrder,
                question.getOptions().stream().map(QuestionAttemptOptionResponse::from).collect(Collectors.toList())
        );
    }
}
