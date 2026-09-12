package com.examforge.exam.service;

import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamCategory;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.dto.ExamRequest;
import com.examforge.exam.dto.ExamResponse;
import com.examforge.exam.repository.ExamCategoryRepository;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.exam.repository.ExamSpecifications;
import com.examforge.subject.repository.SubjectRepository;
import com.examforge.test.repository.TestPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamCategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final TestPaperRepository testRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> searchPublic(UUID categoryId, ExamDifficulty difficulty, String keyword, Pageable pageable) {
        Specification<Exam> spec = Specification
                .where(ExamSpecifications.hasStatus(ExamStatus.PUBLISHED))
                .and(ExamSpecifications.hasCategory(categoryId))
                .and(ExamSpecifications.hasDifficulty(difficulty))
                .and(ExamSpecifications.nameContains(keyword));

        return examRepository.findAll(spec, pageable).map(ExamResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getPublicById(UUID id) {
        Exam exam = examRepository.findById(id)
                .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", id));
        return ExamResponse.from(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> searchAdmin(UUID categoryId, ExamDifficulty difficulty, ExamStatus status, String keyword, Pageable pageable) {
        Specification<Exam> spec = Specification
                .where(ExamSpecifications.hasStatus(status))
                .and(ExamSpecifications.hasCategory(categoryId))
                .and(ExamSpecifications.hasDifficulty(difficulty))
                .and(ExamSpecifications.nameContains(keyword));

        return examRepository.findAll(spec, pageable).map(ExamResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResponse getAdminById(UUID id) {
        return ExamResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public ExamResponse create(ExamRequest request) {
        Exam exam = new Exam();
        applyRequest(exam, request);
        if (exam.getStatus() == null) {
            exam.setStatus(ExamStatus.DRAFT);
        }
        return ExamResponse.from(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamResponse update(UUID id, ExamRequest request) {
        Exam exam = findEntity(id);
        applyRequest(exam, request);
        return ExamResponse.from(examRepository.save(exam));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Exam exam = findEntity(id);

        if (subjectRepository.existsByExamId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete this exam while subjects are still attached to it");
        }
        if (testRepository.existsByExamId(id)) {
            throw new ResourceInUseException(
                    "Cannot delete this exam while tests are still attached to it");
        }

        exam.setDeletedAt(Instant.now());
        examRepository.save(exam);
    }

    private void applyRequest(Exam exam, ExamRequest request) {
        ExamCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Exam category", request.getCategoryId()));
        }

        exam.setCategory(category);
        exam.setName(request.getName().trim());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalQuestions(request.getTotalQuestions());
        exam.setMaximumMarks(request.getMaximumMarks());
        exam.setNegativeMarking(request.getNegativeMarking());
        exam.setDifficulty(request.getDifficulty());
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
    }

    private Exam findEntity(UUID id) {
        return examRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", id));
    }
}
