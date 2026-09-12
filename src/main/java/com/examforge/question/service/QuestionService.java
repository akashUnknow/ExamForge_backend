package com.examforge.question.service;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.question.domain.QuestionStatus;
import com.examforge.question.domain.QuestionType;
import com.examforge.question.dto.QuestionRequest;
import com.examforge.question.dto.QuestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QuestionService {

    Page<QuestionResponse> search(UUID topicId, UUID subjectId, QuestionStatus status,
                                   ExamDifficulty difficulty, QuestionType type, String keyword, Pageable pageable);

    QuestionResponse getById(UUID id);

    QuestionResponse create(QuestionRequest request);

    QuestionResponse update(UUID id, QuestionRequest request);

    void delete(UUID id);

    QuestionResponse submitForReview(UUID id);

    QuestionResponse approve(UUID id);

    QuestionResponse reject(UUID id, String reason);

    QuestionResponse publish(UUID id);

    QuestionResponse archive(UUID id);
}
