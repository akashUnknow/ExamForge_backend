package com.examforge.question.service;

import com.examforge.common.exception.DuplicateResourceException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.question.domain.Question;
import com.examforge.question.domain.QuestionTranslation;
import com.examforge.question.dto.QuestionTranslationRequest;
import com.examforge.question.dto.QuestionTranslationResponse;
import com.examforge.question.repository.QuestionRepository;
import com.examforge.question.repository.QuestionTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionTranslationServiceImpl implements QuestionTranslationService {

    private final QuestionTranslationRepository translationRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionTranslationResponse> getByQuestion(UUID questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw ResourceNotFoundException.of("Question", questionId);
        }
        return translationRepository.findByQuestionId(questionId).stream()
                .map(QuestionTranslationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionTranslationResponse create(UUID questionId, QuestionTranslationRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Question", questionId));

        String language = request.getLanguage().trim().toLowerCase();
        if (translationRepository.existsByQuestionIdAndLanguageIgnoreCase(questionId, language)) {
            throw new DuplicateResourceException("A translation for language '" + language + "' already exists");
        }

        QuestionTranslation translation = new QuestionTranslation();
        translation.setQuestion(question);
        translation.setLanguage(language);
        translation.setQuestionText(request.getQuestionText());
        translation.setExplanation(request.getExplanation());

        return QuestionTranslationResponse.from(translationRepository.save(translation));
    }

    @Override
    @Transactional
    public void delete(UUID questionId, UUID translationId) {
        QuestionTranslation translation = translationRepository.findById(translationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Question translation", translationId));

        if (!translation.getQuestion().getId().equals(questionId)) {
            throw ResourceNotFoundException.of("Question translation", translationId);
        }

        translationRepository.delete(translation);
    }
}
