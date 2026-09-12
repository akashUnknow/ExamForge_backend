package com.examforge.exam.service;

import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.dto.ExamRequest;
import com.examforge.exam.dto.ExamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExamService {

    /** Public catalog search - always restricted to PUBLISHED exams. */
    Page<ExamResponse> searchPublic(UUID categoryId, ExamDifficulty difficulty, String keyword, Pageable pageable);

    /** Public single-exam lookup - throws not-found unless the exam is PUBLISHED. */
    ExamResponse getPublicById(UUID id);

    /** Admin search across all statuses. */
    Page<ExamResponse> searchAdmin(UUID categoryId, ExamDifficulty difficulty, ExamStatus status, String keyword, Pageable pageable);

    /** Admin single-exam lookup - any status. */
    ExamResponse getAdminById(UUID id);

    ExamResponse create(ExamRequest request);

    ExamResponse update(UUID id, ExamRequest request);

    void delete(UUID id);
}
