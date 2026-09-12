package com.examforge.question.validation;

import com.examforge.common.exception.BusinessException;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.dto.QuestionOptionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enforces the shape rules for each question type's answer key. These are
 * business rules about answer-key integrity, not simple field presence, so
 * they live here rather than as Bean Validation annotations on the DTO.
 */
@Component
public class QuestionAnswerValidator {

    public void validate(QuestionType type, List<QuestionOptionRequest> options, BigDecimal correctNumericAnswer) {
        switch (type) {
            case NUMERIC -> validateNumeric(correctNumericAnswer);
            case TRUE_FALSE -> validateTrueFalse(options);
            case MCQ -> validateSingleCorrect(options);
            case MULTIPLE_CHOICE -> validateMultipleChoice(options);
        }
    }

    private void validateNumeric(BigDecimal correctNumericAnswer) {
        if (correctNumericAnswer == null) {
            throw fail("A NUMERIC question requires correctNumericAnswer");
        }
    }

    private void validateTrueFalse(List<QuestionOptionRequest> options) {
        if (options == null || options.size() != 2) {
            throw fail("A TRUE_FALSE question requires exactly 2 options");
        }
        requireExactlyOneCorrect(options);
    }

    private void validateSingleCorrect(List<QuestionOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw fail("An MCQ question requires at least 2 options");
        }
        requireExactlyOneCorrect(options);
    }

    private void validateMultipleChoice(List<QuestionOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw fail("A MULTIPLE_CHOICE question requires at least 2 options");
        }
        long correctCount = options.stream().filter(QuestionOptionRequest::isCorrect).count();
        if (correctCount < 1) {
            throw fail("A MULTIPLE_CHOICE question requires at least 1 correct option");
        }
    }

    private void requireExactlyOneCorrect(List<QuestionOptionRequest> options) {
        long correctCount = options.stream().filter(QuestionOptionRequest::isCorrect).count();
        if (correctCount != 1) {
            throw fail("Exactly one option must be marked correct");
        }
    }

    private BusinessException fail(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }
}
