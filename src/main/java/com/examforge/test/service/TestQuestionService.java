package com.examforge.test.service;

import com.examforge.test.dto.AutoGenerateRequest;
import com.examforge.test.dto.TestQuestionAttachRequest;
import com.examforge.test.dto.TestQuestionResponse;
import com.examforge.test.dto.TestQuestionUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface TestQuestionService {

    List<TestQuestionResponse> getByTest(UUID testId);

    TestQuestionResponse attach(UUID testId, TestQuestionAttachRequest request);

    TestQuestionResponse update(UUID testId, UUID testQuestionId, TestQuestionUpdateRequest request);

    void detach(UUID testId, UUID testQuestionId);

    /** Randomly fills the test from PUBLISHED questions matching each bucket's filters. All-or-nothing. */
    List<TestQuestionResponse> autoGenerate(UUID testId, AutoGenerateRequest request);
}
