package com.examforge.test.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.dto.TestPaperRequest;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.test.repository.TestPaperSpecifications;
import com.examforge.test.repository.TestQuestionRepository;
import com.examforge.test.repository.TestSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestPaperServiceImpl implements TestPaperService {

    private final TestPaperRepository testRepository;
    private final ExamRepository examRepository;
    private final TestSectionRepository testSectionRepository;
    private final TestQuestionRepository testQuestionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TestPaperResponse> searchPublic(UUID examId, Boolean free, String keyword, Pageable pageable) {
        Specification<TestPaper> spec = Specification
                .where(TestPaperSpecifications.hasStatus(TestStatus.PUBLISHED))
                .and(TestPaperSpecifications.examIsPublished())
                .and(TestPaperSpecifications.hasExam(examId))
                .and(TestPaperSpecifications.isFree(free))
                .and(TestPaperSpecifications.nameContains(keyword));

        return testRepository.findAll(spec, pageable).map(TestPaperResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TestPaperResponse getPublicById(UUID id) {
        TestPaper test = testRepository.findById(id)
                .filter(t -> t.getStatus() == TestStatus.PUBLISHED && t.getExam().getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", id));
        return TestPaperResponse.from(test);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestPaperResponse> searchAdmin(UUID examId, TestStatus status, Boolean free, String keyword, Pageable pageable) {
        Specification<TestPaper> spec = Specification
                .where(TestPaperSpecifications.hasStatus(status))
                .and(TestPaperSpecifications.hasExam(examId))
                .and(TestPaperSpecifications.isFree(free))
                .and(TestPaperSpecifications.nameContains(keyword));

        return testRepository.findAll(spec, pageable).map(TestPaperResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TestPaperResponse getAdminById(UUID id) {
        return TestPaperResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public TestPaperResponse create(TestPaperRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));

        validatePricing(request);

        TestPaper test = new TestPaper();
        test.setExam(exam);
        test.setStatus(TestStatus.DRAFT);
        applyRequest(test, request);

        return TestPaperResponse.from(testRepository.save(test));
    }

    @Override
    @Transactional
    public TestPaperResponse update(UUID id, TestPaperRequest request) {
        TestPaper test = findEntity(id);
        validatePricing(request);

        if (!test.getExam().getId().equals(request.getExamId())) {
            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));
            test.setExam(exam);
        }

        applyRequest(test, request);
        return TestPaperResponse.from(testRepository.save(test));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        TestPaper test = findEntity(id);

        if (testQuestionRepository.countByTestId(id) > 0) {
            throw new ResourceInUseException("Cannot delete this test while questions are still attached to it");
        }
        if (testSectionRepository.existsByTestId(id)) {
            throw new ResourceInUseException("Cannot delete this test while sections are still attached to it");
        }

        test.setDeletedAt(Instant.now());
        testRepository.save(test);
    }

    private void validatePricing(TestPaperRequest request) {
        if (!request.isFree() && request.getPrice() == null) {
            throw new BusinessException("Price is required for a paid test", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyRequest(TestPaper test, TestPaperRequest request) {
        test.setName(request.getName().trim());
        test.setDescription(request.getDescription());
        test.setDurationMinutes(request.getDurationMinutes());
        test.setTotalMarks(request.getTotalMarks());
        test.setNegativeMarking(request.getNegativeMarking());
        test.setFree(request.isFree());
        test.setPrice(request.isFree() ? null : request.getPrice());
        if (request.getStatus() != null) {
            test.setStatus(request.getStatus());
        }
    }

    private TestPaper findEntity(UUID id) {
        return testRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", id));
    }
}
