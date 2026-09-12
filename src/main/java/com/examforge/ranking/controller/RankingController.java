package com.examforge.ranking.controller;

import com.examforge.common.response.ApiResponse;
import com.examforge.common.response.PageResponse;
import com.examforge.ranking.dto.RankingResponse;
import com.examforge.ranking.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rankings", description = "Public per-test leaderboard")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/api/v1/tests/{testId}/ranking")
    @Operation(summary = "Leaderboard for a published test - one entry per user, their best score")
    public ApiResponse<PageResponse<RankingResponse>> getRanking(
            @PathVariable UUID testId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(rankingService.getRanking(testId, pageable));
    }
}
