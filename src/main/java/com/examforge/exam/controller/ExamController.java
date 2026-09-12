package com.examforge.exam.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.exam.domain.ExamDifficulty;
import com.examforge.exam.dto.ExamResponse;
import com.examforge.exam.service.ExamService;
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
 * Public exam catalog. Always restricted to PUBLISHED exams - draft and
 * archived exams are only visible through the admin endpoints.
 */
@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@Tag(name = "Exams", description = "Public exam catalog")
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @Operation(summary = "List published exams", description = "Supports pagination, filtering by category/difficulty/keyword, and sorting")
    public ApiResponse<PageResponse<ExamResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ExamDifficulty difficulty,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(
                PageResponse.from(examService.searchPublic(categoryId, difficulty, keyword, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published exam by id")
    public ApiResponse<ExamResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(examService.getPublicById(id));
    }
}
