package com.examforge.ranking.service;

import com.examforge.common.response.PageResponse;
import com.examforge.ranking.dto.RankingResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RankingService {

    /** Only for PUBLISHED tests under a PUBLISHED exam - same visibility gate as the rest of the public catalog. */
    PageResponse<RankingResponse> getRanking(UUID testId, Pageable pageable);
}
