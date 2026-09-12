package com.examforge.topic.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.topic.dto.TopicResponse;
import com.examforge.topic.service.TopicService;
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

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Public topic listing, nested under a subject")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "List topics for a subject whose exam is published")
    public ApiResponse<PageResponse<TopicResponse>> list(
            @PathVariable UUID subjectId,
            @PageableDefault(size = 50, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(topicService.getPublicBySubject(subjectId, pageable)));
    }
}
