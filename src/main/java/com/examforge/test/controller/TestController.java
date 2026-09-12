package com.examforge.test.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.test.dto.TestPaperResponse;
import com.examforge.test.service.TestPaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public test catalog - metadata only (name, duration, marks, pricing).
 * Deliberately does NOT expose the question list; that's the Attempt
 * module's job (a later phase), which must serve an answer-free question
 * shape rather than reusing question.dto.QuestionResponse.
 */
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Tag(name = "Tests", description = "Public test catalog")
public class TestController {

    private final TestPaperService testService;

    @GetMapping
    @Operation(summary = "List published tests", description = "Only tests whose parent exam is also published are visible")
    public ApiResponse<PageResponse<TestPaperResponse>> list(
            @RequestParam(required = false) UUID examId,
            @RequestParam(required = false) Boolean free,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(testService.searchPublic(examId, free, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published test by id")
    public ApiResponse<TestPaperResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(testService.getPublicById(id));
    }
}
