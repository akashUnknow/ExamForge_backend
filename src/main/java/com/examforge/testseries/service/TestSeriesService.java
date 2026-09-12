package com.examforge.testseries.service;

import com.examforge.test.domain.TestStatus;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.testseries.dto.TestSeriesRequest;
import com.examforge.testseries.dto.TestSeriesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TestSeriesService {

    Page<TestSeriesResponse> searchPublic(UUID examId, String keyword, Pageable pageable);

    TestSeriesResponse getPublicById(UUID id);

    /** Metadata only for the tests bundled in this series - individual test access is still governed by each test's own gate. */
    List<TestPaperResponse> getPublicTests(UUID seriesId);

    Page<TestSeriesResponse> searchAdmin(UUID examId, TestStatus status, String keyword, Pageable pageable);

    TestSeriesResponse getAdminById(UUID id);

    TestSeriesResponse create(TestSeriesRequest request);

    TestSeriesResponse update(UUID id, TestSeriesRequest request);

    void delete(UUID id);

    void attachTest(UUID seriesId, UUID testId, Integer displayOrder);

    void detachTest(UUID seriesId, UUID testId);
}
