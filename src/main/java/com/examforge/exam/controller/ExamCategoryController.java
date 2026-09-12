package com.examforge.exam.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.exam.dto.ExamCategoryResponse;
import com.examforge.exam.service.ExamCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-categories")
@RequiredArgsConstructor
@Tag(name = "Exam Categories", description = "Public exam category listing")
public class ExamCategoryController {

    private final ExamCategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all exam categories")
    public ApiResponse<List<ExamCategoryResponse>> list() {
        return ApiResponse.success(categoryService.getAll());
    }
}
