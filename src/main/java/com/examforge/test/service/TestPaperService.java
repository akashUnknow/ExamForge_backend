package com.examforge.test.service;

import com.examforge.test.domain.TestStatus;
import com.examforge.test.dto.TestPaperRequest;
import com.examforge.test.dto.TestPaperResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TestPaperService {

    /** Public: only PUBLISHED tests whose parent exam is also PUBLISHED. */
    Page<TestPaperResponse> searchPublic(UUID examId, Boolean free, String keyword, Pageable pageable);

    TestPaperResponse getPublicById(UUID id);

    /** Admin: any status. */
    Page<TestPaperResponse> searchAdmin(UUID examId, TestStatus status, Boolean free, String keyword, Pageable pageable);

    TestPaperResponse getAdminById(UUID id);

    TestPaperResponse create(TestPaperRequest request);

    TestPaperResponse update(UUID id, TestPaperRequest request);

    void delete(UUID id);
}
