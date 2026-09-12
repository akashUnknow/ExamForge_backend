package com.examforge.testseries.service;

import com.examforge.common.exception.BusinessException;
import com.examforge.common.exception.ResourceInUseException;
import com.examforge.common.exception.ResourceNotFoundException;
import com.examforge.exam.domain.Exam;
import com.examforge.exam.domain.ExamStatus;
import com.examforge.exam.repository.ExamRepository;
import com.examforge.test.domain.TestPaper;
import com.examforge.test.domain.TestStatus;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.test.repository.TestPaperRepository;
import com.examforge.testseries.domain.TestSeries;
import com.examforge.testseries.domain.TestSeriesTest;
import com.examforge.testseries.dto.TestSeriesRequest;
import com.examforge.testseries.dto.TestSeriesResponse;
import com.examforge.testseries.repository.TestSeriesRepository;
import com.examforge.testseries.repository.TestSeriesSpecifications;
import com.examforge.testseries.repository.TestSeriesTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestSeriesServiceImpl implements TestSeriesService {

    private final TestSeriesRepository seriesRepository;
    private final TestSeriesTestRepository seriesTestRepository;
    private final ExamRepository examRepository;
    private final TestPaperRepository testRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TestSeriesResponse> searchPublic(UUID examId, String keyword, Pageable pageable) {
        Specification<TestSeries> spec = Specification
                .where(TestSeriesSpecifications.hasStatus(TestStatus.PUBLISHED))
                .and(TestSeriesSpecifications.examIsPublished())
                .and(TestSeriesSpecifications.hasExam(examId))
                .and(TestSeriesSpecifications.nameContains(keyword));

        return seriesRepository.findAll(spec, pageable).map(TestSeriesResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSeriesResponse getPublicById(UUID id) {
        TestSeries series = seriesRepository.findById(id)
                .filter(s -> s.getStatus() == TestStatus.PUBLISHED && s.getExam().getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Test series", id));
        return TestSeriesResponse.from(series);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestPaperResponse> getPublicTests(UUID seriesId) {
        // Reuses the same visibility gate as getPublicById - a series's
        // test list is only browsable once the series itself is public.
        getPublicById(seriesId);

        return seriesTestRepository.findByTestSeriesIdOrderByDisplayOrderAsc(seriesId).stream()
                .map(TestSeriesTest::getTest)
                .map(TestPaperResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestSeriesResponse> searchAdmin(UUID examId, TestStatus status, String keyword, Pageable pageable) {
        Specification<TestSeries> spec = Specification
                .where(TestSeriesSpecifications.hasStatus(status))
                .and(TestSeriesSpecifications.hasExam(examId))
                .and(TestSeriesSpecifications.nameContains(keyword));

        return seriesRepository.findAll(spec, pageable).map(TestSeriesResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSeriesResponse getAdminById(UUID id) {
        return TestSeriesResponse.from(findEntity(id));
    }

    @Override
    @Transactional
    public TestSeriesResponse create(TestSeriesRequest request) {
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));

        validatePricing(request);

        TestSeries series = new TestSeries();
        series.setExam(exam);
        series.setStatus(TestStatus.DRAFT);
        applyRequest(series, request);

        return TestSeriesResponse.from(seriesRepository.save(series));
    }

    @Override
    @Transactional
    public TestSeriesResponse update(UUID id, TestSeriesRequest request) {
        TestSeries series = findEntity(id);
        validatePricing(request);

        if (!series.getExam().getId().equals(request.getExamId())) {
            Exam exam = examRepository.findById(request.getExamId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Exam", request.getExamId()));
            series.setExam(exam);
        }

        applyRequest(series, request);
        return TestSeriesResponse.from(seriesRepository.save(series));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        TestSeries series = findEntity(id);

        if (seriesTestRepository.countByTestSeriesId(id) > 0) {
            throw new ResourceInUseException("Cannot delete this series while tests are still attached to it");
        }

        series.setDeletedAt(Instant.now());
        seriesRepository.save(series);
    }

    @Override
    @Transactional
    public void attachTest(UUID seriesId, UUID testId, Integer displayOrder) {
        TestSeries series = findEntity(seriesId);
        TestPaper test = testRepository.findById(testId)
                .orElseThrow(() -> ResourceNotFoundException.of("Test", testId));

        if (seriesTestRepository.existsByTestSeriesIdAndTestId(seriesId, testId)) {
            throw new BusinessException("This test is already part of the series", HttpStatus.CONFLICT);
        }

        TestSeriesTest link = new TestSeriesTest();
        link.setTestSeries(series);
        link.setTest(test);
        link.setDisplayOrder(displayOrder != null ? displayOrder : (int) seriesTestRepository.countByTestSeriesId(seriesId) + 1);

        seriesTestRepository.save(link);
    }

    @Override
    @Transactional
    public void detachTest(UUID seriesId, UUID testId) {
        List<TestSeriesTest> links = seriesTestRepository.findByTestSeriesIdOrderByDisplayOrderAsc(seriesId);
        TestSeriesTest link = links.stream()
                .filter(l -> l.getTest().getId().equals(testId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Test in series", testId));

        seriesTestRepository.delete(link);
    }

    private void validatePricing(TestSeriesRequest request) {
        if (!request.isFree() && request.getPrice() == null) {
            throw new BusinessException("Price is required for a paid test series", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyRequest(TestSeries series, TestSeriesRequest request) {
        series.setName(request.getName().trim());
        series.setDescription(request.getDescription());
        series.setFree(request.isFree());
        series.setPrice(request.isFree() ? null : request.getPrice());
        series.setStartDate(request.getStartDate());
        series.setEndDate(request.getEndDate());
        series.setAccessDurationDays(request.getAccessDurationDays());
        if (request.getStatus() != null) {
            series.setStatus(request.getStatus());
        }
    }

    private TestSeries findEntity(UUID id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Test series", id));
    }
}
