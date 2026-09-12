package com.examforge.test.service;

import com.examforge.test.dto.TestSectionRequest;
import com.examforge.test.dto.TestSectionResponse;

import java.util.List;
import java.util.UUID;

public interface TestSectionService {

    List<TestSectionResponse> getByTest(UUID testId);

    TestSectionResponse create(UUID testId, TestSectionRequest request);

    TestSectionResponse update(UUID testId, UUID sectionId, TestSectionRequest request);

    void delete(UUID testId, UUID sectionId);
}
