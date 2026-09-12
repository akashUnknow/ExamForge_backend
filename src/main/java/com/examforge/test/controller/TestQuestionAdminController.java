package com.examforge.test.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.test.dto.AutoGenerateRequest;
import com.examforge.test.dto.TestQuestionAttachRequest;
import com.examforge.test.dto.TestQuestionResponse;
import com.examforge.test.dto.TestQuestionUpdateRequest;
import com.examforge.test.service.TestQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Attaches, reorders, and detaches questions on a test, plus random
 * auto-generation from the question bank. Manual attach and auto-generate
 * both only ever pull PUBLISHED questions - see requirePublished() in
 * TestQuestionServiceImpl.
 */
@RestController
@RequestMapping("/api/v1/admin/tests/{testId}/questions")
@RequiredArgsConstructor
@Tag(name = "Admin - Tests", description = "Admin test question assembly (manual and automatic)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class TestQuestionAdminController {

    private final TestQuestionService testQuestionService;

    @GetMapping
    @Operation(summary = "List questions attached to a test, including their answer keys")
    public ApiResponse<List<TestQuestionResponse>> list(@PathVariable UUID testId) {
        return ApiResponse.success(testQuestionService.getByTest(testId));
    }

    @PostMapping
    @Operation(summary = "Manually attach a single PUBLISHED question to the test")
    public ResponseEntity<ApiResponse<TestQuestionResponse>> attach(
            @PathVariable UUID testId, @Valid @RequestBody TestQuestionAttachRequest request) {
        TestQuestionResponse response = testQuestionService.attach(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question attached successfully", response));
    }

    @PutMapping("/{testQuestionId}")
    @Operation(summary = "Update a question's section, order, or marks override on this test")
    public ApiResponse<TestQuestionResponse> update(
            @PathVariable UUID testId, @PathVariable UUID testQuestionId,
            @Valid @RequestBody TestQuestionUpdateRequest request) {
        return ApiResponse.success("Question updated successfully",
                testQuestionService.update(testId, testQuestionId, request));
    }

    @DeleteMapping("/{testQuestionId}")
    @Operation(summary = "Detach a question from the test")
    public ApiResponse<Void> detach(@PathVariable UUID testId, @PathVariable UUID testQuestionId) {
        testQuestionService.detach(testId, testQuestionId);
        return ApiResponse.success("Question detached successfully", null);
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "Randomly fill the test from PUBLISHED questions matching subject/topic + difficulty buckets",
            description = "All-or-nothing: if any bucket can't be fully satisfied, no questions are added and a 409 is returned")
    public ApiResponse<List<TestQuestionResponse>> autoGenerate(
            @PathVariable UUID testId, @Valid @RequestBody AutoGenerateRequest request) {
        return ApiResponse.success("Questions generated successfully",
                testQuestionService.autoGenerate(testId, request));
    }
}
