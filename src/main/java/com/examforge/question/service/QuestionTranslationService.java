package com.examforge.question.service;

import com.examforge.question.dto.QuestionTranslationRequest;
import com.examforge.question.dto.QuestionTranslationResponse;

import java.util.List;
import java.util.UUID;

public interface QuestionTranslationService {

    List<QuestionTranslationResponse> getByQuestion(UUID questionId);

    QuestionTranslationResponse create(UUID questionId, QuestionTranslationRequest request);

    void delete(UUID questionId, UUID translationId);
}
