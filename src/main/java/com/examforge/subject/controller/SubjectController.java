package com.examforge.subject.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.subject.dto.SubjectResponse;
import com.examforge.subject.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public subject listing, nested under a published exam. A subject list
 * for a draft/archived exam returns 404, matching the exam's own visibility.
 */
@RestController
@RequestMapping("/api/v1/exams/{examId}/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Public subject listing, nested under an exam")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "List subjects for a published exam")
    public ApiResponse<PageResponse<SubjectResponse>> list(
            @PathVariable UUID examId,
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(subjectService.getPublicByExam(examId, pageable)));
    }
}
